import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      navigate('/', { replace: true });
    } catch {
      setError('No se pudo iniciar sesión con privilegios de administrador.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-glow" />
      <form className="login-card" onSubmit={onSubmit}>
        <p className="eyebrow">Acceso seguro</p>
        <h1>Panel Admin</h1>
        <p className="subtitle">Gestión de usuarios, chats, grupos y círculos de confianza.</p>

        <label htmlFor="email">Correo</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="admin@empresa.com"
          required
        />

        <label htmlFor="password">Contraseña</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="••••••••"
          required
        />

        {error ? <p className="form-error">{error}</p> : null}

        <button type="submit" disabled={loading} className="primary-btn">
          {loading ? 'Validando...' : 'Entrar'}
        </button>
      </form>
    </div>
  );
}
