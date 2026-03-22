import { useEffect, useState } from 'react';
import { deletePrivateChat, getPrivateChats } from '../api/admin';
import type { PrivateChat } from '../types';

export function PrivateChatsPage() {
  const [items, setItems] = useState<PrivateChat[]>([]);

  const load = async () => {
    setItems(await getPrivateChats());
  };

  useEffect(() => {
    void load();
  }, []);

  const onDelete = async (id: number) => {
    await deletePrivateChat(id);
    await load();
  };

  return (
    <section>
      <div className="page-heading">
        <h3>Gestión de chats privados</h3>
        <p>Revisión y eliminación de conversaciones uno a uno.</p>
      </div>
      <div className="table-wrap">
        <table>
          <thead><tr><th>ID</th><th>Usuarios</th><th>Acción</th></tr></thead>
          <tbody>
            {items.map((chat) => (
              <tr key={chat.id}>
                <td>{chat.id}</td>
                <td>{chat.userIds?.join(' - ') || '-'}</td>
                <td><button className="danger-btn" onClick={() => onDelete(chat.id)}>Eliminar</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
