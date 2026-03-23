import { useEffect, useState } from 'react';
import { deleteGroupChat, getGroupChats } from '../api/admin';
import type { GroupChat } from '../types';

export function GroupChatsPage() {
  const [items, setItems] = useState<GroupChat[]>([]);

  const load = async () => {
    setItems(await getGroupChats());
  };

  useEffect(() => {
    void load();
  }, []);

  const onDelete = async (id: number) => {
    await deleteGroupChat(id);
    await load();
  };

  return (
    <section>
      <div className="page-heading">
        <h3>Gestión de chats de grupo</h3>
        <p>Supervisa grupos existentes y elimina conversaciones no válidas.</p>
      </div>
      <div className="table-wrap">
        <table>
          <thead><tr><th>ID</th><th>Participantes</th><th>Acción</th></tr></thead>
          <tbody>
            {items.map((chat) => (
              <tr key={chat.id}>
                <td>{chat.id}</td>
                <td>{chat.userIds?.join(', ') || '-'}</td>
                <td><button className="danger-btn" onClick={() => onDelete(chat.id)}>Eliminar</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
