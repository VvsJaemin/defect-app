import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import dynamicImport from 'vite-plugin-dynamic-import'

export default defineConfig(({ mode }) => {
    return {
        plugins: [react(), dynamicImport()],
        assetsInclude: ['**/*.md'],
        resolve: {
            alias: {
                '@': path.join(__dirname, 'src'),
            },
        },
        server: {
            proxy: {
                '/auth': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                },
                '/users': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                },
                '/defects': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                },
                '/defectLogs': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                },
                '/projects': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                },
                '/files': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                },
            },
        },
        build: {
            outDir: 'dist',
        },
        define: {
            'process.env': {},
        },
    }
})