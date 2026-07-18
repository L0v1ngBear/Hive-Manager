import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const hiveRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');

function read(relativePath) {
  return fs.readFileSync(path.join(hiveRoot, relativePath), 'utf8');
}

const retiredStandaloneClaims = [
  /微信(?:一键)?登录[\s\S]{0,120}(?:查找|匹配)[\s\S]{0,80}(?:创建|新建|新增)[\s\S]{0,40}(?:用户|账号)/u,
  /(?:没有|尚未|未)[\s\S]{0,30}(?:加入|所属)[\s\S]{0,20}组织[\s\S]{0,100}只能[\s\S]{0,60}(?:加入组织入口|不能使用业务功能)/u,
  /(?:WeChat|mini-program)[\s\S]{0,120}(?:find|match)[\s\S]{0,80}(?:create|provision)[\s\S]{0,40}(?:user|account)/iu,
  /(?:authenticated|logged in)[\s\S]{0,100}(?:without|no)[\s\S]{0,30}(?:tenant|organization)/iu
];

test('manual describes employee matching and invitation joining only', () => {
  const manual = read('management-ui/src/views/manual/UserManual.vue');

  assert.doesNotMatch(manual, /新手机号可先登录/);
  for (const retiredClaim of retiredStandaloneClaims) {
    assert.doesNotMatch(manual, retiredClaim);
  }
  assert.match(
    manual,
    /微信手机号用于匹配企业员工账号；未建档用户需由管理员添加，或使用管理员生成的有效组织邀请码加入。/
  );
});

test('production-registered baseline remains pinned while living guidance carries retirement policy', () => {
  const baseline = read('db-migrations/baseline/hive_schema_baseline_v2.sql');
  const currentGuidance = {
    manual: read('management-ui/src/views/manual/UserManual.vue'),
    java: read('management/src/main/java/my/hive/domain/employee/service/EmployeeService.java'),
    migration: read('db-migrations/migrations/V20260718_001_unified_employee_login.sql')
  };

  assert.equal(
    createHash('sha256').update(baseline).digest('hex'),
    'd99de67e80de5170588af4c1c7923ebd57994a15866becdb7d96fea94e59bd7c'
  );
  for (const [sourceName, source] of Object.entries(currentGuidance)) {
    for (const retiredClaim of retiredStandaloneClaims) {
      assert.doesNotMatch(source, retiredClaim, `${sourceName} retains standalone login guidance`);
    }
  }
  assert.match(currentGuidance.migration, /retires the standalone mini-program login model/i);
  assert.match(currentGuidance.manual, /微信手机号用于匹配企业员工账号/u);
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
