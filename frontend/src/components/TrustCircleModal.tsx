import { useEffect, useState } from 'react';
import { getUsers } from '../api/admin';
import type { User, TrustCircle } from '../types';

interface TrustCircleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (name: string, userIds: number[]) => Promise<void>;
  circle?: TrustCircle | null;
}

export function TrustCircleModal({ isOpen, onClose, onSave, circle }: TrustCircleModalProps) {
  const [name, setName] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<Set<number>>(new Set());
  const [searchEmail, setSearchEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      loadUsers();
      if (circle) {
        setName(circle.name);
        setSelectedUsers(new Set(circle.members?.map((m) => m.id) || []));
      } else {
        setName('');
        setSelectedUsers(new Set());
      }
      setSearchEmail('');
      setError('');
    }
  }, [isOpen, circle]);

  const loadUsers = async () => {
    try {
      const allUsers = await getUsers();
      setUsers(allUsers);
    } catch (err) {
      setError('Error al cargar usuarios');
      console.error(err);
    }
  };

  const handleToggleUser = (userId: number) => {
    const newSelected = new Set(selectedUsers);
    if (newSelected.has(userId)) {
      newSelected.delete(userId);
    } else {
      newSelected.add(userId);
    }
    setSelectedUsers(newSelected);
  };

  const handleSave = async () => {
    if (!name.trim()) {
      setError('El nombre del círculo es obligatorio');
      return;
    }

    if (selectedUsers.size === 0) {
      setError('Debes añadir al menos un usuario al círculo');
      return;
    }

    try {
      setIsLoading(true);
      await onSave(name, Array.from(selectedUsers));
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar el círculo');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const filteredUsers = users.filter((user) =>
    user.email.toLowerCase().includes(searchEmail.toLowerCase())
  );

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{circle ? 'Editar círculo de confianza' : 'Crear círculo de confianza'}</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        <div className="modal-body">
          <div className="form-group">
            <label htmlFor="circle-name">Nombre del círculo</label>
            <input
              id="circle-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ej: Administración"
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="user-search">Buscar usuarios por email</label>
            <input
              id="user-search"
              type="text"
              value={searchEmail}
              onChange={(e) => setSearchEmail(e.target.value)}
              placeholder="Escribe un email para buscar..."
              disabled={isLoading}
            />
          </div>

          <div className="users-list">
            <h4>Usuarios seleccionados ({selectedUsers.size})</h4>
            {selectedUsers.size > 0 ? (
              <div className="selected-users">
                {Array.from(selectedUsers)
                  .map((id) => users.find((u) => u.id === id))
                  .filter(Boolean)
                  .map((user) => (
                    <div key={user!.id} className="user-tag">
                      <span>{user!.email}</span>
                      <button
                        type="button"
                        className="user-tag-remove"
                        onClick={() => handleToggleUser(user!.id)}
                        disabled={isLoading}
                      >
                        ×
                      </button>
                    </div>
                  ))}
              </div>
            ) : (
              <p className="text-muted">Ningún usuario seleccionado</p>
            )}
          </div>

          <div className="users-search-results">
            <h4>Resultados de búsqueda</h4>
            {filteredUsers.length > 0 ? (
              <div className="user-list">
                {filteredUsers.map((user) => (
                  <div
                    key={user.id}
                    className={`user-item ${selectedUsers.has(user.id) ? 'selected' : ''}`}
                    onClick={() => handleToggleUser(user.id)}
                  >
                    <input
                      type="checkbox"
                      checked={selectedUsers.has(user.id)}
                      onChange={() => handleToggleUser(user.id)}
                      disabled={isLoading}
                    />
                    <div className="user-info">
                      <div className="user-name">{user.name}</div>
                      <div className="user-email">{user.email}</div>
                    </div>
                  </div>
                ))}
              </div>
            ) : searchEmail.length > 0 ? (
              <p className="text-muted">No se encontraron usuarios</p>
            ) : (
              <p className="text-muted">Escribe un email para buscar usuarios</p>
            )}
          </div>

          {error && <div className="form-error">{error}</div>}
        </div>

        <div className="modal-footer">
          <button className="secondary-btn" onClick={onClose} disabled={isLoading}>
            Cancelar
          </button>
          <button className="primary-btn" onClick={handleSave} disabled={isLoading}>
            {isLoading ? 'Guardando...' : circle ? 'Actualizar' : 'Crear'}
          </button>
        </div>
      </div>
    </div>
  );
}
