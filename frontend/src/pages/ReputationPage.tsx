import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, ApiError } from '../api';
import type { LedgerEntry, ReputationProfile } from '../types';

/** 声誉档案页：/users/:id —— 声誉值、徽章人人可见；悬赏积分与流水仅本人/管理员可见。 */
export function ReputationPage() {
  const { id } = useParams();
  const userId = Number(id);
  const [profile, setProfile] = useState<ReputationProfile | null>(null);
  const [ledger, setLedger] = useState<LedgerEntry[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!Number.isInteger(userId) || userId <= 0) {
      setError('用户 id 不合法');
      setLoading(false);
      return;
    }
    let alive = true;
    setLoading(true);
    setError('');
    setProfile(null);
    setLedger([]);

    api.reputation(userId)
      .then((p) => {
        if (!alive) return undefined;
        setProfile(p);
        // availablePoints 为 null 表示「非本人且非管理员」。流水接口对同样的角色开放，
        // 所以用它当开关，避免为他人页面白跑一次必然 403 的请求。
        if (p.availablePoints == null) return undefined;
        return api.ledger(userId)
          .then((r) => { if (alive) setLedger(r.items); })
          .catch(() => undefined);
      })
      .catch((e) => {
        if (!alive) return;
        // REP-4001：该用户还没有声誉档案（例如从未登录过的新账号）
        setError(e instanceof ApiError && e.status === 404 ? '该用户还没有声誉档案' : e.message);
      })
      .finally(() => { if (alive) setLoading(false); });

    return () => { alive = false; };
  }, [userId]);

  if (loading) return <p className="muted">加载中…</p>;
  if (error) return <p className="error">{error}</p>;
  if (!profile) return <p className="muted">暂无数据。</p>;

  const pointsVisible = profile.availablePoints != null;

  return (
    <div className="form-page">
      <p className="muted"><Link to="/leaderboard">← 返回排行榜</Link></p>

      <h1>
        {profile.displayName}
        <span className="muted"> 的声誉</span>
      </h1>
      <p>
        声誉值 <b>{profile.reputation}</b>
      </p>

      {pointsVisible ? (
        <table className="board">
          <thead><tr><th>可用悬赏积分</th><th>冻结中</th></tr></thead>
          <tbody>
            <tr>
              <td>{profile.availablePoints}</td>
              <td>{profile.frozenPoints}</td>
            </tr>
          </tbody>
        </table>
      ) : (
        <p className="muted">悬赏积分仅本人与管理员可见。</p>
      )}

      <h2>徽章</h2>
      {profile.badges.length === 0 ? (
        <p className="muted">还没有获得徽章。</p>
      ) : (
        <ul className="plain">
          {profile.badges.map((b) => (
            <li key={b.code}>
              <b>{b.name}</b>
              <span className="muted"> — {b.description}</span>
              <span className="muted"> · {formatTime(b.awardedAt)}</span>
            </li>
          ))}
        </ul>
      )}

      {pointsVisible && (
        <>
          <h2>声誉流水</h2>
          {ledger.length === 0 ? (
            <p className="muted">还没有流水记录。</p>
          ) : (
            <table className="board">
              <thead><tr><th>时间</th><th>原因</th><th>变动</th><th>关联</th></tr></thead>
              <tbody>
                {ledger.map((e) => (
                  <tr key={e.id}>
                    <td className="muted">{formatTime(e.createdAt)}</td>
                    <td>{e.reasonLabel}</td>
                    <td className={e.delta > 0 ? 'verified' : e.delta < 0 ? 'error' : 'muted'}>
                      {e.delta > 0 ? `+${e.delta}` : e.delta}
                    </td>
                    <td className="muted">{e.refType} #{e.refId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}

/** 后端时间可能是 ISO 串；解析失败时返回占位，避免渲染出 NaN / Invalid Date。 */
function formatTime(value: string): string {
  const t = new Date(value);
  return Number.isNaN(t.getTime()) ? '—' : t.toLocaleString('zh-CN');
}
