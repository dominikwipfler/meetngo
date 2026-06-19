const { defineConfig } = require("vitest/config");

module.exports = defineConfig({
  test: {
    environment: "node",
    fileParallelism: false,
    globals: true,
    setupFiles: ["./tests/setup.js"],
  },
});
