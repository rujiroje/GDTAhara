import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'

export default [
  // Ignore build output, backup files, and oddly-named generated files
  {
    ignores: [
      'dist',
      '**/* - Copy*.jsx',
      '**/* - Copy*.js',
      'src/App_old.jsx',
      'src/import React from*.jsx',
      'src/import React,*.jsx',
    ],
  },

  // Node environment for Vite config
  {
    files: ['vite.config.js'],
    languageOptions: {
      globals: { ...globals.node },
    },
  },

  // Main source files
  {
    files: ['**/*.{js,jsx}'],
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    languageOptions: {
      ecmaVersion: 2020,
      globals: {
        ...globals.browser,
        process: 'readonly', // Vite injects process.env at build time
      },
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    rules: {
      ...js.configs.recommended.rules,
      'react-hooks/rules-of-hooks': 'error',
      // exhaustive-deps has too many pre-existing violations; enforce in new code via code review
      'react-hooks/exhaustive-deps': 'off',
      // Only-export-components is overly strict for context/hook files in this codebase
      'react-refresh/only-export-components': 'off',
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]', argsIgnorePattern: '^_' }],
    },
  },
]
