import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import prettier from 'eslint-config-prettier'

export default tseslint.config(
  { ignores: ['dist'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.recommended, prettier],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
    },
    rules: {
      // Only the long-established hooks rules — this codebase doesn't use the
      // React Compiler, so the newer compiler-correctness rules (purity,
      // set-state-in-effect, etc.) don't apply and would flag idiomatic code.
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
  {
    // shadcn/ui primitives and the Auth context conventionally co-export
    // helper constants/hooks alongside components — not worth splitting,
    // so react-refresh's only-export-components rule is scoped to skip them.
    files: ['src/**/*.{ts,tsx}'],
    ignores: ['src/app/components/ui/**', 'src/context/**'],
    plugins: { 'react-refresh': reactRefresh },
    rules: reactRefresh.configs.vite.rules,
  },
)
