import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const hiveRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');

function read(relativePath) {
  return fs.readFileSync(path.join(hiveRoot, relativePath), 'utf8');
}

test('manual describes employee matching and invitation joining only', () => {
  const manual = read('management-ui/src/views/manual/UserManual.vue');
  const retiredStandaloneClaims = [
    /微信(?:一键)?登录[\s\S]{0,120}(?:查找|匹配)[\s\S]{0,80}(?:创建|新建|新增)[\s\S]{0,40}(?:用户|账号)/u,
    /(?:没有|尚未|未)[\s\S]{0,30}(?:加入|所属)[\s\S]{0,20}组织[\s\S]{0,100}只能[\s\S]{0,60}(?:加入组织入口|不能使用业务功能)/u
  ];

  assert.doesNotMatch(manual, /新手机号可先登录/);
  for (const retiredClaim of retiredStandaloneClaims) {
    assert.doesNotMatch(manual, retiredClaim);
  }
  assert.match(
    manual,
    /微信手机号用于匹配企业员工账号；未建档用户需由管理员添加，或使用管理员生成的有效组织邀请码加入。/
  );
});

test('baseline keeps nullable legacy rows without documenting standalone authentication', () => {
  const baseline = read('db-migrations/baseline/hive_schema_baseline_v2.sql');
  const userTable = baseline.match(/CREATE TABLE `user` \([\s\S]*?\n\)/u)?.[0] || '';

  assert.match(userTable, /`tenant_code`\s+varchar\(50\)\s+DEFAULT NULL/u);
  assert.match(userTable, /`tenant_code`[^\n]*COMMENT\s+'[^']*(?:遗留|历史)[^']*(?:核对|兼容)[^']*'/u);
  assert.doesNotMatch(
    userTable,
    /`tenant_code`[^\n]*COMMENT\s+'[^']*(?:微信|WeChat)[^']*(?:未加入|无组织|为空|blank)[^']*'/iu
  );
});

test('EmployeeService comments do not describe retired standalone or shared-password behavior', () => {
  const employeeService = read('management/src/main/java/my/hive/domain/employee/service/EmployeeService.java');

  assert.doesNotMatch(employeeService, /小程序一键登录会先产生一条没有\s*emp_employee_ext\s*档案的\s*user/);
  assert.doesNotMatch(employeeService, /新员工初始密码从配置读取/);
});

test('new migration documents retirement without rewriting historical migration', () => {
  const migration = read('db-migrations/migrations/V20260718_001_unified_employee_login.sql');

  assert.match(migration, /retires the standalone mini-program login model/i);
  assert.match(migration, /V20260506_001_mini_wechat_standalone_login\.sql/);
});
