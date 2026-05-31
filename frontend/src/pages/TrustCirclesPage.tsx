import { useEffect, useState } from 'react';
import { deleteTrustCircle, getTrustCircles, createTrustCircle, addUserToCircle, removeUserFromCircle } from '../api/admin';
import { TrustCircleModal } from '../components/TrustCircleModal';
import type { TrustCircle } from '../types';

export function TrustCirclesPage() {
  const [items, setItems] = useState<TrustCircle[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedCircle, setSelectedCircle] = useState<TrustCircle | null>(null);
  const [expandedCircleId, setExpandedCircleId] = useState<number | null>(null);

  const load = async () => {
    setItems(await getTrustCircles());
  };

  useEffect(() => {
    void load();
  }, []);

  const onDelete = async (id: number) => {
    if (confirm('¿Estás seguro de que deseas eliminar este círculo de confianza?')) {
      await deleteTrustCircle(id);
      await load();
    }
  };

  const handleCreateClick = () => {
    setSelectedCircle(null);
    setIsModalOpen(true);
  };

  const handleEditClick = (circle: TrustCircle) => {
    setSelectedCircle(circle);
    setIsModalOpen(true);
  };

  const handleSaveCircle = async (name: string, userIds: number[]) => {
    if (selectedCircle) {
      // Editar círculo existente - añadir/remover usuarios
      const currentUserIds = new Set(selectedCircle.members?.map((m) => m.id) || []);
      const newUserIds = new Set(userIds);

      // Añadir usuarios nuevos
      for (const userId of newUserIds) {
        if (!currentUserIds.has(userId)) {
          await addUserToCircle(selectedCircle.id, userId);
        }
      }

      // Remover usuarios que se deseleccionaron
      for (const userId of currentUserIds) {
        if (!newUserIds.has(userId)) {
          await removeUserFromCircle(selectedCircle.id, userId);
        }
      }
    } else {
      // Crear nuevo círculo
      await createTrustCircle(name, userIds);
    }
    await load();
  };

  return (
    <section>
      <div className="page-heading">
        <div className="heading-with-actions">
          <div>
            <h3>Gestión de círculos de confianza</h3>
            <p>Control de dominios, miembros y depuración administrativa.</p>
          </div>
          <div className="heading-actions">
            <button className="primary-btn" onClick={handleCreateClick}>
              + Crear círculo
            </button>
          </div>
        </div>
      </div>

      <TrustCircleModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveCircle}
        circle={selectedCircle}
      />

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Miembros</th>
              <th>Consent Domain</th>
              <th>Acción</th>
            </tr>
          </thead>
          <tbody>
            {items.map((circle) => (
              <tr key={circle.id}>
                <td>{circle.id}</td>
                <td>{circle.name}</td>
                <td>
                  <div className="members-cell">
                    <span className="member-count">{circle.members?.length || 0} usuario(s)</span>
                    <button
                      className="expand-btn"
                      onClick={() => setExpandedCircleId(expandedCircleId === circle.id ? null : circle.id)}
                    >
                      {expandedCircleId === circle.id ? '▼' : '▶'}
                    </button>
                  </div>
                  {expandedCircleId === circle.id && (
                    <div className="members-list">
                      {circle.members?.map((m) => (
                        <div key={m.id} className="member-item">
                          {m.name} ({m.email})
                        </div>
                      ))}
                    </div>
                  )}
                </td>
                <td>{circle.consentDomain ? 'Sí' : 'No'}</td>
                <td>
                  <div className="action-buttons">
                    <button
                      className="secondary-btn"
                      onClick={() => handleEditClick(circle)}
                      title="Editar usuarios del círculo"
                    >
                      ✎ Editar
                    </button>
                    <button
                      className="danger-btn"
                      onClick={() => onDelete(circle.id)}
                    >
                      Eliminar
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
