import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Allow both standard Vite env vars (VITE_SUPABASE_*) and existing SUPABASE_* env vars
  envPrefix: ['VITE_', 'SUPABASE_'],
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
  server: {
    host: true,
    port: 5173,
  },
});
