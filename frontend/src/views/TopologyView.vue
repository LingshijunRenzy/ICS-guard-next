<template>
  <div class="topology-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-success"><ShareIcon size="22px" /></span>
        <h2>{{ $t('topology.title') }}</h2>
      </div>
      <div class="header-actions">
        <t-tag v-for="d in legendData" :key="d.type" variant="light" class="legend-tag">
          <span class="legend-dot" :style="{ background: d.color }" />{{ d.label }}
        </t-tag>
        <t-button variant="outline" @click="resetView">
          <template #icon><ZoomOutIcon /></template>
          {{ $t('topology.resetView') }}
        </t-button>
        <t-button theme="primary" :loading="loading" @click="fetchTopology">
          <template #icon><RefreshIcon /></template>
          {{ $t('common.refresh') }}
        </t-button>
      </div>
    </div>

    <!-- 3D 拓扑画布 -->
    <t-card :bordered="false" class="section-card topo-card">
      <div ref="containerRef" class="topology-3d-container" :class="currentTheme"></div>
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ShareIcon, RefreshIcon, ZoomOutIcon } from 'tdesign-icons-vue-next'
import { useI18n } from 'vue-i18n'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { get } from '@/api/client'
import { currentTheme } from '@/theme/index'
import {
  CANVAS_WIDTH,
  CANVAS_HEIGHT,
  NODE_WIDTH,
  NODE_HEIGHT,
  ZONES,
  DEVICES,
  BACKBONE_CONNECTIONS,
  INTRACONNECTIONS
} from '@/topology/config'
import '@/styles/topology.css'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const containerRef = ref<HTMLElement | null>(null)

/** 在线设备状态缓存 */
const deviceStatuses = ref<Record<string, string>>({})
/** 有告警的设备ID集合 */
const alertDeviceIds = ref<Set<string>>(new Set())

const legendData = computed(() => [
  { type: 'online', label: t('deviceStatus.online'), color: '#00c853' },
  { type: 'alert', label: t('topology.hasAlert'), color: '#ff5252' },
])

const DEVICE_COLORS: Record<string, string> = {
  PLC: '#FFA500',       // Orange
  SCADA: '#0064FF',     // Blue
  HMI: '#00FF9C',       // Green
  RTU: '#2DFEFF',       // Cyan
  Protection: '#FF0000',// Red
  Workstation: '#E020F0'// Purple
}

// 两套配色方案
const THEMES = {
  dark: {
    bg: '#0a0e27',
    zonePlane: 0x112244,
    zoneBorder: 0x335588,
    zoneText: '#66b3ff',
    deviceBorder: 0xffffff,
    deviceCore: 0xffffff,
    deviceText: 'white',
    deviceTextShadow: 'rgba(0, 0, 0, 0.8)',
    ambientLight: 0.7,
    dirLight: 0.5
  },
  light: {
    bg: '#f0f2f5',
    zonePlane: 0xe2e8f0,
    zoneBorder: 0x94a3b8,
    zoneText: '#334155',
    deviceBorder: 0x475569,
    deviceCore: 0x94a3b8,
    deviceText: '#1e293b',
    deviceTextShadow: 'rgba(255, 255, 255, 0.8)',
    ambientLight: 0.9,
    dirLight: 0.3
  }
}

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let animationId: number
let ambientLight: THREE.AmbientLight
let dirLight: THREE.DirectionalLight

// 用来存储材质，方便切换主题时统一更新
const sceneMaterials = {
  zonePlanes: [] as THREE.MeshBasicMaterial[],
  zoneBorders: [] as THREE.LineBasicMaterial[],
  zoneSprites: [] as { sprite: THREE.Sprite, key: string }[],
  deviceBorders: [] as THREE.LineBasicMaterial[],
  deviceCores: [] as THREE.MeshBasicMaterial[],
  deviceSprites: [] as { sprite: THREE.Sprite, name: string }[]
}

const updateTheme = () => {
  if (!scene) return
  const isDark = currentTheme.value === 'dark'
  const colors = isDark ? THEMES.dark : THEMES.light

  scene.background = new THREE.Color(colors.bg)
  ambientLight.intensity = colors.ambientLight
  dirLight.intensity = colors.dirLight

  sceneMaterials.zonePlanes.forEach(m => m.color.setHex(colors.zonePlane))
  sceneMaterials.zoneBorders.forEach(m => m.color.setHex(colors.zoneBorder))
  
  sceneMaterials.zoneSprites.forEach(item => {
    updateTextSprite(item.sprite, t(`topology.zones.${item.key}`), colors.zoneText, 24, 'left')
  })

  sceneMaterials.deviceBorders.forEach(m => m.color.setHex(colors.deviceBorder))
  sceneMaterials.deviceCores.forEach(m => m.color.setHex(colors.deviceCore))

  sceneMaterials.deviceSprites.forEach(item => {
    updateDeviceSprite(item.sprite, item.name, colors.deviceText, colors.deviceTextShadow)
  })
}

watch(currentTheme, () => {
  updateTheme()
})

const initThree = () => {
  if (!containerRef.value) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight || 600

  scene = new THREE.Scene()
  
  const aspect = width / height
  camera = new THREE.PerspectiveCamera(60, aspect, 1, 10000)
  camera.position.set(CANVAS_WIDTH / 2, 1000, CANVAS_HEIGHT / 2 + 600)
  camera.lookAt(CANVAS_WIDTH / 2, 0, CANVAS_HEIGHT / 2)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  containerRef.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.target.set(CANVAS_WIDTH / 2, 0, CANVAS_HEIGHT / 2)

  ambientLight = new THREE.AmbientLight(0xffffff, 0.7)
  scene.add(ambientLight)
  dirLight = new THREE.DirectionalLight(0xffffff, 0.5)
  dirLight.position.set(0, 1000, 500)
  scene.add(dirLight)

  createZones()

  DEVICES.forEach(device => {
    const group = createDeviceNode(device)
    scene.add(group)
  })

  createLinks()
  
  // 初始化主题色彩
  updateTheme()

  window.addEventListener('resize', onWindowResize)
  animate()
}

const createZones = () => {
  ZONES.forEach(zone => {
    const geometry = new THREE.PlaneGeometry(zone.width, zone.height)
    const material = new THREE.MeshBasicMaterial({ transparent: true, opacity: 0.4, side: THREE.DoubleSide })
    sceneMaterials.zonePlanes.push(material)

    const plane = new THREE.Mesh(geometry, material)
    plane.rotation.x = -Math.PI / 2
    plane.position.set(zone.x + zone.width / 2, -2, zone.y + zone.height / 2)
    scene.add(plane)

    const edges = new THREE.EdgesGeometry(geometry)
    const lineMat = new THREE.LineBasicMaterial({ linewidth: 2 })
    sceneMaterials.zoneBorders.push(lineMat)

    const wireframe = new THREE.LineSegments(edges, lineMat)
    wireframe.rotation.x = -Math.PI / 2
    wireframe.position.copy(plane.position)
    scene.add(wireframe)
    
    const textSprite = new THREE.Sprite(new THREE.SpriteMaterial({ transparent: true }))
    textSprite.scale.set(160, 40, 1)
    textSprite.position.set(zone.x + 80, 5, zone.y + 30)
    sceneMaterials.zoneSprites.push({ sprite: textSprite, key: zone.nameKey })
    scene.add(textSprite)
  })
}

const updateTextSprite = (sprite: THREE.Sprite, text: string, color: string, fontSize: number, align: CanvasTextAlign) => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (ctx) {
    canvas.width = 256
    canvas.height = 64
    // 每次绘制前清空画布
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.font = `bold ${fontSize}px Arial`
    ctx.fillStyle = color
    ctx.textAlign = align
    ctx.textBaseline = 'middle'
    ctx.fillText(text, 10, 32)
  }
  const texture = new THREE.CanvasTexture(canvas)
  texture.minFilter = THREE.LinearFilter
  texture.needsUpdate = true
  if (sprite.material.map) {
    sprite.material.map.dispose()
  }
  sprite.material.map = texture
}

const updateDeviceSprite = (sprite: THREE.Sprite, name: string, color: string, shadow: string) => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (ctx) {
    canvas.width = 512
    canvas.height = 128
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.font = 'bold 36px Arial'
    ctx.fillStyle = color
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.shadowColor = shadow
    ctx.shadowBlur = 8
    ctx.fillText(name, 256, 64)
  }
  const texture = new THREE.CanvasTexture(canvas)
  texture.minFilter = THREE.LinearFilter
  texture.needsUpdate = true
  if (sprite.material.map) {
    sprite.material.map.dispose()
  }
  sprite.material.map = texture
}

const createDeviceNode = (device: typeof DEVICES[0]) => {
  const group = new THREE.Group()

  const size = 50
  const height = 25

  const type = device.type
  const baseColorHex = DEVICE_COLORS[type] || '#808080'
  const baseColor = new THREE.Color(baseColorHex)

  // 1. 线框
  const geometry = new THREE.BoxGeometry(size, height, size)
  const edges = new THREE.EdgesGeometry(geometry)
  const borderMaterial = new THREE.LineBasicMaterial({ transparent: true, opacity: 0.8 })
  sceneMaterials.deviceBorders.push(borderMaterial)
  const wireframe = new THREE.LineSegments(edges, borderMaterial)
  group.add(wireframe)

  // 2. 内部方块 半透明
  const coreGeo = new THREE.BoxGeometry(size * 0.7, height * 0.5, size * 0.7)
  const coreMat = new THREE.MeshBasicMaterial({ transparent: true, opacity: 0.8 })
  sceneMaterials.deviceCores.push(coreMat)
  const core = new THREE.Mesh(coreGeo, coreMat)
  group.add(core)

  // 3. 底部发光面
  const planeGeo = new THREE.PlaneGeometry(size - 2, size - 2)
  const planeMat = new THREE.MeshBasicMaterial({
    color: baseColor,
    side: THREE.DoubleSide,
    transparent: true,
    opacity: 0.6
  })
  const plane = new THREE.Mesh(planeGeo, planeMat)
  plane.rotation.x = -Math.PI / 2
  plane.position.y = -height / 2 + 0.1
  group.add(plane)

  // 4. 文字标签
  const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ transparent: true, depthTest: false }))
  sprite.scale.set(120, 30, 1)
  sprite.position.y = size * 0.6
  sceneMaterials.deviceSprites.push({ sprite, name: device.name })
  group.add(sprite)

  group.position.set(device.x + NODE_WIDTH / 2, height / 2, device.y + NODE_HEIGHT / 2)

  return group
}

const createLinks = () => {
  const addLine = (fromId: string, toId: string, color: number) => {
    const fromDev = DEVICES.find(d => d.deviceId === fromId)
    const toDev = DEVICES.find(d => d.deviceId === toId)
    if (!fromDev || !toDev) return

    const x1 = fromDev.x + NODE_WIDTH / 2
    const z1 = fromDev.y + NODE_HEIGHT / 2
    const x2 = toDev.x + NODE_WIDTH / 2
    const z2 = toDev.y + NODE_HEIGHT / 2

    const zMid = (z1 + z2) / 2
    const points = []
    points.push(new THREE.Vector3(x1, 0, z1))
    points.push(new THREE.Vector3(x1, 0, zMid))
    points.push(new THREE.Vector3(x2, 0, zMid))
    points.push(new THREE.Vector3(x2, 0, z2))

    const geo = new THREE.BufferGeometry().setFromPoints(points)
    const customMat = new THREE.LineBasicMaterial({ color, opacity: 0.6, transparent: true })
    const line = new THREE.Line(geo, customMat)
    scene.add(line)
  }

  BACKBONE_CONNECTIONS.forEach(c => addLine(c.from, c.to, 0x00c853))
  INTRACONNECTIONS.forEach(c => addLine(c.from, c.to, 0x3b82f6))
}

const animate = () => {
  animationId = requestAnimationFrame(animate)
  controls.update()
  renderer.render(scene, camera)
}

const onWindowResize = () => {
  if (!containerRef.value || !camera || !renderer) return
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight || 600
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

const resetView = () => {
  if (!camera || !controls) return
  camera.position.set(CANVAS_WIDTH / 2, 1000, CANVAS_HEIGHT / 2 + 600)
  camera.lookAt(CANVAS_WIDTH / 2, 0, CANVAS_HEIGHT / 2)
  controls.target.set(CANVAS_WIDTH / 2, 0, CANVAS_HEIGHT / 2)
  controls.update()
}

const fetchTopology = async () => {
  loading.value = true
  try {
    // API
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initThree()
})

onUnmounted(() => {
  // 释放资源
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onWindowResize)
  
  if (sceneMaterials.zonePlanes) {
    sceneMaterials.zonePlanes.forEach(m => m.dispose())
    sceneMaterials.zoneBorders.forEach(m => m.dispose())
    sceneMaterials.deviceBorders.forEach(m => m.dispose())
    sceneMaterials.deviceCores.forEach(m => m.dispose())
    sceneMaterials.zoneSprites.forEach(s => s.sprite.material.map?.dispose())
    sceneMaterials.deviceSprites.forEach(s => s.sprite.material.map?.dispose())
  }
  
  if (renderer) {
    renderer.dispose()
  }
})
</script>

<style scoped>
.topology-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.topo-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  background-color: var(--td-bg-color-container);
}
.topology-3d-container {
  flex: 1;
  width: 100%;
  min-height: 600px;
  position: relative;
  transition: background 0.3s ease;
}
.topology-3d-container.dark {
  background: linear-gradient(135deg, #0a0e27, #1a1a2e);
}
.topology-3d-container.light {
  background: linear-gradient(135deg, #e2e8f0, #f8fafc);
}
</style>
