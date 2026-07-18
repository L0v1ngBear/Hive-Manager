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

  assert.doesNotMatch(manual, /新手机号可先登录/);
  assert.doesNotMatch(manual, /微信一键登录会查找或创建用户信息/);
  assert.match(
    manual,
    /微信手机号用于匹配企业员工账号；未建档用户需由管理员添加，或使用管理员生成的有效组织邀请码加入。/
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
