import { useEffect, useState } from 'react';
import api from '../api/client';

interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  userId: number;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}

interface User {
  id: number;
  name: string;
  email: string;
  emailVerified: boolean;
  adminApproved: boolean;
  role: string;
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [pendingUsers, setPendingUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedNotification, setSelectedNotification] = useState<Notification | null>(null);
  const [actionReason, setActionReason] = useState('');

  useEffect(() => {
    loadNotifications();
    loadPendingUsers();
  }, []);

  const loadNotifications = async () => {
    try {
      const response = await api.get('/api/notifications');
      setNotifications(response.data);
    } catch (err) {
      console.error('Error cargando notificaciones:', err);
    }
  };

  const loadPendingUsers = async () => {
    try {
      const response = await api.get('/api/users');
      // Filtrar usuarios que verificaron email pero no fueron aprobados
      const pending = response.data.filter((u: User) => u.emailVerified && !u.adminApproved);
      setPendingUsers(pending);
      setLoading(false);
    } catch (err) {
      console.error('Error cargando usuarios pendientes:', err);
      setLoading(false);
    }
  };

  const handleApproveUser = async (userId: number) => {
    try {
      await api.post(`/api/auth/users/${userId}/approve`, {});
      loadPendingUsers();
      setSelectedNotification(null);
    } catch (err: any) {
      alert('Error al aprobar usuario: ' + (err.response?.data?.error || err.message));
    }
  };

  const handleRejectUser = async (userId: number) => {
    if (!actionReason.trim()) {
      alert('Por favor ingresa una razón para rechazar');
      return;
    }
    
    try {
      await api.post(`/api/auth/users/${userId}/reject`, { reason: actionReason });
      loadPendingUsers();
      setSelectedNotification(null);
      setActionReason('');
    } catch (err: any) {
      alert('Error al rechazar usuario: ' + (err.response?.data?.error || err.message));
    }
  };

  const markAsRead = async (notificationId: number) => {
    try {
      const response = await api.put(`/api/notifications/${notificationId}/read`, {});
      setNotifications(prev => 
        prev.map(n => n.id === notificationId ? response.data : n)
      );
    } catch (err) {
      console.error('Error marcando notificación como leída:', err);
    }
  };

  const deleteNotification = async (notificationId: number) => {
    try {
      await api.delete(`/api/notifications/${notificationId}`);
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
    } catch (err) {
      console.error('Error eliminando notificación:', err);
    }
  };

  if (loading) {
    return <div style={{ padding: '20px' }}>Cargando...</div>;
  }

  const unreadCount = notifications.filter(n => !n.read).length;

  return (
    <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
      <h1>Centro de Notificaciones</h1>

      {/* Usuariosendientes de aprobación */}
      <section style={{ marginBottom: '30px' }}>
        <h2>Usuarios Pendientes de Aprobación ({pendingUsers.length})</h2>
        
        {pendingUsers.length === 0 ? (
          <p style={{ color: '#666' }}>No hay usuarios pendientes de aprobación.</p>
        ) : (
          <div style={{ display: 'grid', gap: '10px' }}>
            {pendingUsers.map(user => (
              <div
                key={user.id}
                style={{
                  padding: '15px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  backgroundColor: '#f9f9f9'
                }}
              >
                <div style={{ marginBottom: '10px' }}>
                  <h3 style={{ margin: '0 0 5px 0' }}>{user.name}</h3>
                  <p style={{ margin: '0', color: '#666' }}>{user.email}</p>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    onClick={() => handleApproveUser(user.id)}
                    style={{
                      padding: '8px 16px',
                      backgroundColor: '#28a745',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer'
                    }}
                  >
                    Aprobar
                  </button>
                  <button
                    onClick={() => {
                      setSelectedNotification({
                        id: user.id,
                        type: 'REJECT',
                        title: 'Rechazar',
                        message: '',
                        userId: user.id,
                        read: false,
                        createdAt: '',
                        readAt: null
                      });
                    }}
                    style={{
                      padding: '8px 16px',
                      backgroundColor: '#dc3545',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer'
                    }}
                  >
                    Rechazar
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Modal para rechazar */}
      {selectedNotification?.type === 'REJECT' && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            padding: '20px',
            borderRadius: '8px',
            maxWidth: '400px',
            width: '90%'
          }}>
            <h3>Rechazar Usuario</h3>
            <textarea
              value={actionReason}
              onChange={(e) => setActionReason(e.target.value)}
              placeholder="Razón del rechazo"
              style={{
                width: '100%',
                minHeight: '100px',
                padding: '10px',
                marginBottom: '15px',
                border: '1px solid #ddd',
                borderRadius: '4px',
                boxSizing: 'border-box'
              }}
            />
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setSelectedNotification(null)}
                style={{
                  padding: '8px 16px',
                  backgroundColor: '#6c757d',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Cancelar
              </button>
              <button
                onClick={() => handleRejectUser(selectedNotification.userId)}
                style={{
                  padding: '8px 16px',
                  backgroundColor: '#dc3545',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Confirmar Rechazo
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Notificaciones */}
      <section>
        <h2>Notificaciones ({unreadCount} sin leer)</h2>
        
        {notifications.length === 0 ? (
          <p style={{ color: '#666' }}>No hay notificaciones.</p>
        ) : (
          <div style={{ display: 'grid', gap: '10px' }}>
            {notifications.map(notification => (
              <div
                key={notification.id}
                style={{
                  padding: '15px',
                  border: '1px solid ' + (notification.read ? '#ddd' : '#007bff'),
                  borderRadius: '4px',
                  backgroundColor: notification.read ? '#f9f9f9' : '#e7f3ff',
                  borderLeft: '4px solid ' + (notification.read ? '#ddd' : '#007bff')
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                  <div>
                    <h4 style={{ margin: '0 0 5px 0' }}>{notification.title}</h4>
                    <p style={{ margin: '0 0 8px 0', color: '#666' }}>{notification.message}</p>
                    <small style={{ color: '#999' }}>
                      {new Date(notification.createdAt).toLocaleString()}
                    </small>
                  </div>
                  <div style={{ display: 'flex', gap: '5px' }}>
                    {!notification.read && (
                      <button
                        onClick={() => markAsRead(notification.id)}
                        style={{
                          padding: '5px 10px',
                          backgroundColor: '#007bff',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontSize: '0.85em'
                        }}
                      >
                        Marcar como leído
                      </button>
                    )}
                    <button
                      onClick={() => deleteNotification(notification.id)}
                      style={{
                        padding: '5px 10px',
                        backgroundColor: '#dc3545',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '0.85em'
                      }}
                    >
                      Eliminar
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
