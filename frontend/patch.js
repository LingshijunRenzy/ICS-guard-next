const fs = require('fs');
let content = fs.readFileSync('src/layouts/MainLayout.vue', 'utf8');
content = content.replace('  color: var(--td-text-color-primary)\n.custom-menu {', '  color: var(--td-text-color-primary);\n}\n\n.custom-menu {');
fs.writeFileSync('src/layouts/MainLayout.vue', content);
