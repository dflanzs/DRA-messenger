import { useEffect, useState } from 'react';
import { deleteMessage, getMessages } from '../api/admin';
import type { Message } from '../types';

export function MessagesPage() {
  const [items, setItems] = useState<Message[]>([]);

  const load = async () => {
    setItems(await getMessages());
  };

  useEffect(() => {
    void load();
  }, []);

  const onDelete = async (id: number) => {
    await deleteMessage(id);
    await load();
  };

  return (
    <section>
      <div className="page-heading">
        <h3>Gestión de mensajes</h3>
        <p>Auditoría de contenidos y acciones de moderación.</p>
      </div>
      <div className="table-wrap">
        <table>
          <thead><tr><th>ID</th><th>Emisor</th><th>Contenido</th><th>Fecha</th><th>Acción</th></tr></thead>
          <tbody>
            {items.map((message) => (
              <tr key={message.id}>
                <td>{message.id}</td>
                <td>{message.sender?.email || '-'}</td>
                <td>{message.content}</td>
                <td>{new Date(message.createdAt).toLocaleString()}</td>
                <td><button className="danger-btn" onClick={() => onDelete(message.id)}>Eliminar</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
