import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/usuarios', label: 'Usuarios' },
  { to: '/circulos', label: 'Círculos de Confianza' },
  { to: '/auditoria', label: 'Auditoría' },
  { to: '/notificaciones', label: 'Notificaciones' },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const onLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-card">
          <p className="eyebrow">TFG</p>
          <h1>Control de Sistema</h1>
          <p className="subtitle">Panel administrativo</p>
        </div>

        <nav className="nav-menu">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `nav-link ${isActive ? 'nav-link-active' : ''}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <p className="topbar-label">Sesión</p>
            <h2>{user?.name}</h2>
            <p className="topbar-email">{user?.email}</p>
          </div>
          <button className="danger-btn" onClick={onLogout}>Cerrar sesión</button>
        </header>

        <section className="page-container">
          <Outlet />
        </section>
      </main>
    </div>
  );
}
