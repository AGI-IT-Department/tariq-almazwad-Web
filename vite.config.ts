import fs from 'node:fs';
import path from 'node:path';
import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';

function pwaIconsPlugin(): Plugin {
  const iconSource = path.resolve(
    __dirname,
    'app/src/main/res/drawable/ic_challenge_launcher_src.png'
  );
  const iconTargets = ['icon-192.png', 'icon-512.png', 'apple-touch-icon.png'];

  return {
    name: 'tariq-pwa-icons',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const cleanUrl = (req.url || '').split('?')[0].replace(/^\//, '');
        if (iconTargets.includes(cleanUrl) && fs.existsSync(iconSource)) {
          res.setHeader('Content-Type', 'image/png');
          fs.createReadStream(iconSource).pipe(res);
          return;
        }
        next();
      });
    },
    closeBundle() {
      const distDir = path.resolve(__dirname, 'dist');
      if (!fs.existsSync(iconSource) || !fs.existsSync(distDir)) return;
      for (const fileName of iconTargets) {
        fs.copyFileSync(iconSource, path.join(distDir, fileName));
      }
    },
  };
}

export default defineConfig({
  plugins: [react(), pwaIconsPlugin()],
  // Allow both standard Vite env vars (VITE_SUPABASE_*) and existing SUPABASE_* env vars
  envPrefix: ['VITE_', 'SUPABASE_'],
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
  server: {
    host: true,
    port: 5173,
    fs: {
      allow: ['.'],
    },
  },
});
