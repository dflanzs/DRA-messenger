import { useEffect, useState } from 'react';
import { deleteUser, getUsers, updateUserRole } from '../api/admin';
import type { User, UserRole } from '../types';

export function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  const loadUsers = async () => {
    const data = await getUsers();
    setUsers(data);
    setLoading(false);
  };

  useEffect(() => {
    void loadUsers();
  }, []);

  const onRoleChange = async (userId: number, role: UserRole) => {
    await updateUserRole(userId, role);
    await loadUsers();
  };

  const onDelete = async (userId: number) => {
    await deleteUser(userId);
    await loadUsers();
  };

  return (
    <section>
      <div className="page-heading">
        <h3>Gestión de usuarios</h3>
        <p>Administra permisos y elimina cuentas cuando sea necesario.</p>
      </div>
      {loading ? <p>Cargando usuarios...</p> : null}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th><th>Nombre</th><th>Email</th><th>Estado</th><th>Rol</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.onlineStatus ? 'Online' : 'Offline'}</td>
                <td>
                  <select
                    value={user.role}
                    onChange={(event) => onRoleChange(user.id, event.target.value as UserRole)}
                  >
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td>
                  <button className="danger-btn" onClick={() => onDelete(user.id)}>Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
