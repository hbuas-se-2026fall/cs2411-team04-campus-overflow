import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, describeError } from '../api';
import { useAuth } from '../auth';
import type { AdminUserRow, Role } from '../types';

/**
 * 管理页 `/admin`，三个区块一一对应 `AdminUserController` 的端点：
 * 待认证列表（`GET /v1/admin/users?verified=false`）、认证（`POST /{id}/verify`）、
 * 创建教职工（`POST /v1/admin/users`）。
 *
 * <p>这里的 `me.role` 判断<b>不是</b>安全边界——真正的授权在 `SecurityConfig` 的
 * `hasRole("ADMIN")`，后端每次都会再校验一遍。前端这层只为不把必然 403 的页面摆到学生面前。
 */

const PAGE_SIZE = 20;

/** 管理员可创建的角色。学生账号走自助注册（后端 `ID-1005` 也会拒绝代建学生）。 */
const STAFF_ROLES: { value: Role; label: string }[] = [
  { value: 'TA', label: '助教（TA）' },
  { value: 'TEACHER', label: '教师（TEACHER）' },
];

interface StaffForm {
  username: string; displayName: string; email: string; password: string; role: Role; college: string;
}

const EMPTY_FORM: StaffForm = {
  username: '', displayName: '', email: '', password: '', role: 'TA', college: '',
};

export function AdminPage() {
  const { me, loading } = useAuth();
  const isAdmin = me?.role === 'ADMIN';

  const [rows, setRows] = useState<AdminUserRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);

  const [form, setForm] = useState<StaffForm>(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [creating, setCreating] = useState(false);
  const [notice, setNotice] = useState('');

  const load = useCallback(() => {
    if (!isAdmin) {
      setListLoading(false);
      return;
    }
    setListLoading(true);
    setListError('');
    api.adminUsers(false, page, PAGE_SIZE)
      .then((r) => { setRows(r.items); setTotal(r.total); })
      .catch((e) => { setRows([]); setTotal(0); setListError(describeError(e)); })
      .finally(() => setListLoading(false));
  }, [isAdmin, page]);

  useEffect(() => { load(); }, [load]);

  const verify = async (row: AdminUserRow) => {
    setBusyId(row.id);
    setNotice('');
    setListError('');
    try {
      await api.adminVerify(row.id);
      setNotice(`已认证「${row.displayName}」`);
      // 认证后该用户不再属于「待认证」，重新拉一次让表格如实反映服务端状态，
      // 而不是本地删行——本地删行会掩盖「其实没保存成功」这类问题。
      load();
    } catch (e) {
      setListError(describeError(e));
    } finally {
      setBusyId(null);
    }
  };

  const create = async () => {
    setFormError('');
    setNotice('');
    setCreating(true);
    try {
      const { id } = await api.adminCreateStaff({
        username: form.username.trim(),
        displayName: form.displayName.trim(),
        email: form.email.trim(),
        password: form.password,
        role: form.role,
        college: form.college.trim() || undefined,
      });
      setNotice(`已创建教职工账号 #${id}（${form.role}），其悬赏积分账户已自动开户`);
      setForm({ ...EMPTY_FORM, role: form.role });
      load();
    } catch (e) {
      setFormError(describeError(e));
    } finally {
      setCreating(false);
    }
  };

  if (loading) return <p className="muted">加载中…</p>;

  if (!isAdmin) {
    return (
      <div className="form-page">
        <h1>无权访问</h1>
        <p className="muted">
          管理页仅对管理员开放，当前账号角色为 {me ? me.role : '未登录'}。
        </p>
        <p><Link to="/">← 返回问题列表</Link></p>
      </div>
    );
  }

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div className="form-page">
      <h1>管理后台</h1>
      {notice && <p className="verified">{notice}</p>}

      <h2>待认证学生</h2>
      {listLoading ? (
        <p className="muted">加载中…</p>
      ) : listError ? (
        <p className="error">{listError}</p>
      ) : rows.length === 0 ? (
        <p className="muted">没有待认证的学生。</p>
      ) : (
        <table className="board">
          <thead>
            <tr><th>学号</th><th>昵称</th><th>学院</th><th>注册时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td className="muted">{r.maskedUsername}</td>
                <td>{r.displayName}</td>
                <td className="muted">{r.college ?? '—'}</td>
                <td className="muted">{formatTime(r.createdAt)}</td>
                <td>
                  <button onClick={() => verify(r)} disabled={busyId === r.id}>
                    {busyId === r.id ? '认证中…' : '认证'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {!listLoading && !listError && total > PAGE_SIZE && (
        <div className="toolbar">
          <button onClick={() => setPage(page - 1)} disabled={page <= 1}>上一页</button>
          <span className="muted">第 {page} / {totalPages} 页 · 共 {total} 位待认证</span>
          <button onClick={() => setPage(page + 1)} disabled={page >= totalPages}>下一页</button>
        </div>
      )}

      <h2>创建教职工账号</h2>
      <p className="muted">
        学号/工号为 4–20 位字母或数字；密码 8–64 位且须同时含字母与数字；昵称全局唯一。
      </p>
      <label>学号 / 工号
        <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
      </label>
      <label>昵称
        <input maxLength={20} value={form.displayName}
               onChange={(e) => setForm({ ...form, displayName: e.target.value })} />
      </label>
      <label>校园邮箱
        <input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
      </label>
      <label>初始密码
        <input type="password" value={form.password}
               onChange={(e) => setForm({ ...form, password: e.target.value })} />
      </label>
      <label>角色
        <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
          {STAFF_ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
        </select>
      </label>
      <label>学院（可选）
        <input maxLength={50} value={form.college}
               onChange={(e) => setForm({ ...form, college: e.target.value })} />
      </label>
      {formError && <p className="error">{formError}</p>}
      <button className="primary" onClick={create} disabled={creating}>
        {creating ? '创建中…' : '创建账号'}
      </button>
    </div>
  );
}

/** 后端时间可能是 ISO 串；解析失败时返回占位，避免渲染出 NaN / Invalid Date。 */
function formatTime(value: string): string {
  const t = new Date(value);
  return Number.isNaN(t.getTime()) ? '—' : t.toLocaleString('zh-CN');
}
