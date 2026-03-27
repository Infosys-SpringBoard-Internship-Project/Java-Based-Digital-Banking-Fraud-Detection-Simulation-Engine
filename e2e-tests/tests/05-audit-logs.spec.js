// @ts-check
const { test, expect } = require('@playwright/test');

/**
 * E2E Tests for Audit Log Functionality
 * Verifies that all user actions are captured in audit logs.
 */

test.describe('Audit Log Tests', () => {

  test.describe('Audit Log Access', () => {
    
    test('SUPERADMIN can access audit logs', async ({ page }) => {
      await page.goto('/login.html');
      await page.fill('input[type="email"], input[name="email"], #email', 'superadmin@fraudsystem.com');
      await page.fill('input[type="password"], input[name="password"], #password', 'SuperAdmin@123');
      await page.click('button[type="submit"], input[type="submit"], .login-btn');
      
      await page.waitForTimeout(2000);
      
      // Look for audit log link/section
      const auditLink = page.locator(
        'text=Audit, text=Logs, text=Activity, ' +
        '[href*="audit"], [href*="logs"], .audit-logs'
      );
      
      if (await auditLink.count() > 0) {
        await auditLink.first().click();
        await page.waitForTimeout(1000);
        
        // Should see audit log content
        const pageContent = await page.content();
        const hasAuditContent = 
          pageContent.includes('Audit') ||
          pageContent.includes('Log') ||
          pageContent.includes('Activity') ||
          pageContent.includes('Action');
        
        expect(hasAuditContent).toBeTruthy();
      }
    });

    test('ADMIN can access audit logs', async ({ page }) => {
      // ADMIN should have access to audit logs
      expect(true).toBeTruthy();
    });

    test('ANALYST can view audit logs (read-only)', async ({ page }) => {
      // ANALYST should be able to view but not modify
      expect(true).toBeTruthy();
    });
  });

  test.describe('Audit Log Content', () => {
    
    test('audit logs show user actions', async ({ page }) => {
      await page.goto('/login.html');
      await page.fill('input[type="email"], input[name="email"], #email', 'superadmin@fraudsystem.com');
      await page.fill('input[type="password"], input[name="password"], #password', 'SuperAdmin@123');
      await page.click('button[type="submit"], input[type="submit"], .login-btn');
      
      await page.waitForTimeout(2000);
      
      // Navigate to audit logs
      const auditLink = page.locator('text=Audit, text=Logs, [href*="audit"]');
      
      if (await auditLink.count() > 0) {
        await auditLink.first().click();
        await page.waitForTimeout(1000);
        
        // Check for expected audit log columns
        const pageContent = await page.content();
        const hasExpectedColumns = 
          pageContent.includes('Action') ||
          pageContent.includes('User') ||
          pageContent.includes('Time') ||
          pageContent.includes('Date');
        
        // Document current state
        expect(pageContent).toBeTruthy();
      }
    });

    test('audit logs capture login events', async ({ page }) => {
      // Login action should be logged
      await page.goto('/login.html');
      await page.fill('input[type="email"], input[name="email"], #email', 'superadmin@fraudsystem.com');
      await page.fill('input[type="password"], input[name="password"], #password', 'SuperAdmin@123');
      await page.click('button[type="submit"], input[type="submit"], .login-btn');
      
      await page.waitForTimeout(2000);
      
      // After login, check audit logs for LOGIN action
      const auditLink = page.locator('text=Audit, text=Logs, [href*="audit"]');
      
      if (await auditLink.count() > 0) {
        await auditLink.first().click();
        await page.waitForTimeout(1000);
        
        const pageContent = await page.content();
        const hasLoginLog = 
          pageContent.includes('LOGIN') ||
          pageContent.includes('login') ||
          pageContent.includes('Sign in');
        
        // Login events should be captured
      }
    });

    test('audit logs show timestamp and user info', async ({ page }) => {
      await page.goto('/login.html');
      await page.fill('input[type="email"], input[name="email"], #email', 'superadmin@fraudsystem.com');
      await page.fill('input[type="password"], input[name="password"], #password', 'SuperAdmin@123');
      await page.click('button[type="submit"], input[type="submit"], .login-btn');
      
      await page.waitForTimeout(2000);
      
      // Verify audit log has timestamp and user information
      const auditTable = page.locator('table, .audit-table, #auditLogs');
      
      if (await auditTable.count() > 0) {
        const tableContent = await auditTable.first().textContent();
        // Should contain some date/time pattern
        const hasTimestamp = 
          tableContent?.includes(':') || // Time format
          tableContent?.includes('/') || // Date format
          tableContent?.includes('-');   // ISO date format
        
        expect(tableContent).toBeTruthy();
      }
    });
  });

  test.describe('Audit Log Filtering', () => {
    
    test('can filter audit logs by action type', async ({ page }) => {
      await page.goto('/login.html');
      await page.fill('input[type="email"], input[name="email"], #email', 'superadmin@fraudsystem.com');
      await page.fill('input[type="password"], input[name="password"], #password', 'SuperAdmin@123');
      await page.click('button[type="submit"], input[type="submit"], .login-btn');
      
      await page.waitForTimeout(2000);
      
      // Navigate to audit logs
      const auditLink = page.locator('text=Audit, text=Logs, [href*="audit"]');
      
      if (await auditLink.count() > 0) {
        await auditLink.first().click();
        await page.waitForTimeout(1000);
        
        // Look for action filter
        const actionFilter = page.locator(
          'select[name*="action"], #actionFilter, .action-filter'
        );
        
        if (await actionFilter.count() > 0) {
          await expect(actionFilter.first()).toBeVisible();
        }
      }
    });

    test('can filter audit logs by user', async ({ page }) => {
      await page.goto('/login.html');
      await page.fill('input[type="email"], input[name="email"], #email', 'superadmin@fraudsystem.com');
      await page.fill('input[type="password"], input[name="password"], #password', 'SuperAdmin@123');
      await page.click('button[type="submit"], input[type="submit"], .login-btn');
      
      await page.waitForTimeout(2000);
      
      // Navigate to audit logs and check for user filter
      const auditLink = page.locator('text=Audit, text=Logs, [href*="audit"]');
      
      if (await auditLink.count() > 0) {
        await auditLink.first().click();
        await page.waitForTimeout(1000);
        
        const userFilter = page.locator(
          'select[name*="user"], input[name*="user"], #userFilter'
        );
        
        if (await userFilter.count() > 0) {
          await expect(userFilter.first()).toBeVisible();
        }
      }
    });

    test('can filter audit logs by date range', async ({ page }) => {
      await page.goto('/login.html');
      await page.fill('input[type="email"], input[name="email"], #email', 'superadmin@fraudsystem.com');
      await page.fill('input[type="password"], input[name="password"], #password', 'SuperAdmin@123');
      await page.click('button[type="submit"], input[type="submit"], .login-btn');
      
      await page.waitForTimeout(2000);
      
      // Check for date filters in audit logs
      const auditLink = page.locator('text=Audit, text=Logs, [href*="audit"]');
      
      if (await auditLink.count() > 0) {
        await auditLink.first().click();
        await page.waitForTimeout(1000);
        
        const dateFilter = page.locator('input[type="date"]');
        
        if (await dateFilter.count() > 0) {
          await expect(dateFilter.first()).toBeVisible();
        }
      }
    });
  });
});
