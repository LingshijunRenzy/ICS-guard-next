import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'
import prettierConfig from 'eslint-config-prettier'

export default tseslint.config(
  {
    ignores: ['dist/**', 'node_modules/**', 'auto-imports.d.ts', 'components.d.ts'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['*.vue', '**/*.vue'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tseslint.parser,
        sourceType: 'module',
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },
  {
    files: ['*.ts', '**/*.ts'],
    languageOptions: {
      parser: tseslint.parser,
      parserOptions: {
        sourceType: 'module',
      },
    },
  },
  prettierConfig,
  {
    rules: {
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
)
