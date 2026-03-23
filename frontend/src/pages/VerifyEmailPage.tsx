import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { verifyEmail } from '../api/auth';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const runVerification = async () => {
      const token = searchParams.get('token');
      
      if (!token) {
        setStatus('error');
        setMessage('Token de verificación no encontrado');
        return;
      }

      try {
        const response = await verifyEmail({ token });

        setStatus('success');
        setMessage(response.message || 'Correo verificado exitosamente. Tu cuenta está pendiente de aprobación.');
        
        setTimeout(() => {
          navigate('/login');
        }, 3000);
      } catch (err: any) {
        setStatus('error');
        const errorMessage = err.response?.data?.error || 'Error al verificar el correo. Por favor intenta de nuevo.';
        setMessage(errorMessage);
      }
    };

    runVerification();
  }, [searchParams, navigate]);

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      backgroundColor: '#f5f5f5'
    }}>
      <div className="card" style={{ maxWidth: '400px', textAlign: 'center' }}>
        {status === 'loading' && (
          <>
            <h2>Verificando correo...</h2>
            <p>Por favor espera mientras verificamos tu dirección de correo.</p>
          </>
        )}

        {status === 'success' && (
          <>
            <h2 style={{ color: '#3c3' }}>✓ Verificación Exitosa</h2>
            <p>{message}</p>
            <p style={{ color: '#666', fontSize: '0.9em' }}>Redirigiendo al login...</p>
          </>
        )}

        {status === 'error' && (
          <>
            <h2 style={{ color: '#c33' }}>✗ Error de Verificación</h2>
            <p>{message}</p>
            <button
              onClick={() => navigate('/login')}
              style={{
                marginTop: '20px',
                padding: '10px 20px',
                backgroundColor: '#007bff',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Volver al Login
            </button>
          </>
        )}
      </div>
    </div>
  );
}
