import { useEffect, useState } from 'react';
import { getStats } from '../api/admin';
import type { AdminStats } from '../types';

const emptyStats: AdminStats = {
  totalUsers: 0,
  onlineUsers: 0,
  totalMessages: 0,
  totalGroupChats: 0,
  totalPrivateChats: 0,
  totalTrustCircles: 0,
};

export function DashboardPage() {
  const [stats, setStats] = useState<AdminStats>(emptyStats);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getStats();
        setStats(data);
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  return (
    <div>
      <div className="page-heading">
        <h3>Resumen del sistema</h3>
        <p>Visión global del estado actual de la plataforma.</p>
      </div>

      {loading ? <p>Cargando estadísticas...</p> : null}

      <div className="stats-grid">
        <article className="stat-card"><h4>Usuarios activos</h4><strong>{stats.onlineUsers}</strong></article>
        <article className="stat-card"><h4>Usuarios totales</h4><strong>{stats.totalUsers}</strong></article>
        <article className="stat-card"><h4>Círculos de confianza</h4><strong>{stats.totalTrustCircles}</strong></article>
      </div>
    </div>
  );
}
