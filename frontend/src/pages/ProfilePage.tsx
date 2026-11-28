import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, describeError } from '../api';
import { useAuth } from '../auth';

/**
 * 个人资料页 `/profile`。
 *
 * <p>可改的只有昵称、学院、班级三个字段（与后端 `ProfileUpdateRequest` 一一对应）。学号是身份、
 * 角色归管理员、认证状态走审核流程，页面上只展示不可编辑——即便有人改了 DOM 也提交不上去，
 * 后端记录里根本没有这些字段。
 */
export function ProfilePage() {
  const { me, loading, refresh } = useAuth();

  const [displayName, setDisplayName] = useState('');
  const [college, setCollege] = useState('');
  const [className, setClassName] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState('');

  // me 是全局状态，可能在本页挂载后才被 AuthProvider 拉到；每次变化都同步一次表单初值。
  // 依赖里带上三个字段本身，避免「刷新 me 但值没变」时把用户正在输入的内容覆盖掉。
  useEffect(() => {
    if (!me) return;
    setDisplayName(me.displayName);
    setCollege(me.college ?? '');
    setClassName(me.className ?? '');
  }, [me]);

  if (loading) return <p className="muted">加载中…</p>;
  if (!me) {
    return (
      <div className="form-page">
        <h1>请先登录</h1>
        <p><Link to="/login">去登录</Link></p>
      </div>
    );
  }

  const dirty = displayName !== me.displayName
    || college !== (me.college ?? '')
    || className !== (me.className ?? '');

  const save = async () => {
    setError('');
    setSaved('');
    setSaving(true);
    try {
      await api.updateProfile({ displayName, college, className });
      // 必须刷新全局 me：否则顶栏还显示旧昵称，用户会以为没保存成功。
      await refresh();
      setSaved('已保存');
    } catch (e) {
      setError(describeError(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="form-page">
      <h1>我的资料</h1>

      <table className="board">
        <thead>
          <tr><th>学号 / 工号</th><th>角色</th><th>认证状态</th><th>校园邮箱</th></tr>
        </thead>
        <tbody>
          <tr>
            <td>{me.username}</td>
            <td>{me.role}</td>
            <td>{me.verified ? <span className="verified">已认证 ✔</span> : <span className="muted">待认证</span>}</td>
            <td className="muted">{me.email}</td>
          </tr>
        </tbody>
      </table>
      <p className="muted">学号、角色、认证状态由管理员维护，不能自行修改。</p>

      <h2>编辑资料</h2>
      <label>昵称（2–20 字，不能含 @ 或空格，全局唯一）
        <input maxLength={20} value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
      </label>
      <label>学院（可选，最多 50 字）
        <input maxLength={50} value={college} onChange={(e) => setCollege(e.target.value)} />
      </label>
      <label>班级（可选，最多 50 字）
        <input maxLength={50} value={className} onChange={(e) => setClassName(e.target.value)} />
      </label>
      {error && <p className="error">{error}</p>}
      {saved && <p className="verified">{saved}</p>}
      <button className="primary" onClick={save} disabled={saving || !dirty}>
        {saving ? '保存中…' : '保存'}
      </button>

      <h2>所属课程</h2>
      {me.courses.length === 0 ? (
        <p className="muted">还没有加入任何课程。</p>
      ) : (
        <table className="board">
          <thead><tr><th>课程代码</th><th>课程名称</th><th>学期</th><th>任课教师</th></tr></thead>
          <tbody>
            {me.courses.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.name}</td>
                <td className="muted">{c.term}</td>
                <td className="muted">{c.teacherName}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
