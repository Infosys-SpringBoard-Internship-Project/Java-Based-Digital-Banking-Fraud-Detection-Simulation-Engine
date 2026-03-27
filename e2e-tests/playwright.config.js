// @ts-check
const { defineConfig } = require('@playwright/test');

/**
 * Playwright configuration for Fraud Detection System E2E tests.
 * @see https://playwright.dev/docs/test-configuration
 */
module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: false, // Run tests sequentially for user management tests
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1, // Single worker for sequential test execution
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'test-results.json' }]
  ],
  
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { 
        browserName: 'chromium',
        viewport: { width: 1280, height: 720 },
      },
    },
  ],

  // Global timeout for each test
  timeout: 60000,
  
  // Expect timeout
  expect: {
    timeout: 10000,
  },
});
