import { useEffect, useMemo, useState } from 'react';
import { getAuditLogs, getUsers } from '../api/admin';
import type { AuditAction, AuditLog, User } from '../types';

const ACTION_OPTIONS: AuditAction[] = [
  'CREATE_USER',
  'UPDATE_USER',
  'DELETE_USER',
  'REGISTER_USER',
  'VALIDATE_USER',
  'VERIFY_USER_EMAIL',
  'CREATE_GROUP_CHAT',
  'ADD_USER_TO_GROUP_CHAT',
  'REMOVE_USER_FROM_GROUP_CHAT',
  'DELETE_GROUP_CHAT',
  'CREATE_OTO_CHAT',
  'DELETE_OTO_CHAT',
];

const startOfDayMs = (date: string): number => new Date(`${date}T00:00:00`).getTime();
const endOfDayMs = (date: string): number => new Date(`${date}T23:59:59.999`).getTime();

export function AuditPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [userSearch, setUserSearch] = useState('');
  const [action, setAction] = useState<AuditAction | ''>('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  useEffect(() => {
    void getUsers().then(setUsers);
  }, []);

  useEffect(() => {
    const loadLogs = async () => {
      setLoading(true);
      const data = await getAuditLogs({
        userId: selectedUserId ?? undefined,
        action: action || undefined,
        from: fromDate ? startOfDayMs(fromDate) : undefined,
        to: toDate ? endOfDayMs(toDate) : undefined,
      });
      setLogs(data);
      setLoading(false);
    };
    void loadLogs();
  }, [selectedUserId, action, fromDate, toDate]);

  const usersById = useMemo(() => {
    const map = new Map<number, User>();
    users.forEach((user) => map.set(user.id, user));
    return map;
  }, [users]);

  const filteredUsers = useMemo(() => {
    if (selectedUserId !== null || userSearch.trim() === '') return [];
    return users.filter((user) =>
      user.email.toLowerCase().includes(userSearch.toLowerCase())
    );
  }, [users, userSearch, selectedUserId]);

  const onSelectUser = (user: User) => {
    setSelectedUserId(user.id);
    setUserSearch(user.email);
  };

  const onClearUser = () => {
    setSelectedUserId(null);
    setUserSearch('');
  };

  return (
    <section>
      <div className="page-heading">
        <h3>Registro de auditoría</h3>
        <p>Consulta las acciones registradas en el sistema y fíltralas por usuario, fecha y tipo.</p>
      </div>

      <div className="audit-filters">
        <div className="form-group">
          <label htmlFor="audit-user">Usuario</label>
          <input
            id="audit-user"
            type="text"
            value={userSearch}
            onChange={(event) => {
              setUserSearch(event.target.value);
              setSelectedUserId(null);
            }}
            placeholder="Buscar por email..."
            autoComplete="off"
          />
          {selectedUserId !== null ? (
            <button type="button" className="secondary-btn" onClick={onClearUser}>
              Limpiar usuario (ID {selectedUserId})
            </button>
          ) : null}
          {filteredUsers.length > 0 ? (
            <div className="user-list">
              {filteredUsers.map((user) => (
                <div key={user.id} className="user-item" onClick={() => onSelectUser(user)}>
                  <div className="user-info">
                    <div className="user-name">{user.name}</div>
                    <div className="user-email">{user.email}</div>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>

        <div className="form-group">
          <label htmlFor="audit-action">Acción</label>
          <select
            id="audit-action"
            value={action}
            onChange={(event) => setAction(event.target.value as AuditAction | '')}
          >
            <option value="">Todas</option>
            {ACTION_OPTIONS.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="audit-from">Desde</label>
          <input
            id="audit-from"
            type="date"
            value={fromDate}
            onChange={(event) => setFromDate(event.target.value)}
          />
        </div>

        <div className="form-group">
          <label htmlFor="audit-to">Hasta</label>
          <input
            id="audit-to"
            type="date"
            value={toDate}
            onChange={(event) => setToDate(event.target.value)}
          />
        </div>
      </div>

      {loading ? <p>Cargando registros...</p> : null}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th><th>Acción</th><th>Usuario</th><th>Fecha</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => {
              const user = usersById.get(log.userId);
              return (
                <tr key={log.id}>
                  <td>{log.id}</td>
                  <td>{log.action}</td>
                  <td>{user ? `${user.email} (${log.userId})` : log.userId}</td>
                  <td>{new Date(log.timestamp).toLocaleString()}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
