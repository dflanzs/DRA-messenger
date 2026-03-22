import { useEffect, useState } from 'react';
import { deleteTrustCircle, getTrustCircles } from '../api/admin';
import type { TrustCircle } from '../types';

export function TrustCirclesPage() {
  const [items, setItems] = useState<TrustCircle[]>([]);

  const load = async () => {
    setItems(await getTrustCircles());
  };

  useEffect(() => {
    void load();
  }, []);

  const onDelete = async (id: number) => {
    await deleteTrustCircle(id);
    await load();
  };

  return (
    <section>
      <div className="page-heading">
        <h3>Gestión de círculos de confianza</h3>
        <p>Control de dominios, miembros y depuración administrativa.</p>
      </div>
      <div className="table-wrap">
        <table>
          <thead><tr><th>ID</th><th>Nombre</th><th>Miembros</th><th>Consent Domain</th><th>Acción</th></tr></thead>
          <tbody>
            {items.map((circle) => (
              <tr key={circle.id}>
                <td>{circle.id}</td>
                <td>{circle.name}</td>
                <td>{circle.members?.map((m) => m.email).join(', ') || '-'}</td>
                <td>{circle.consentDomain ? 'Sí' : 'No'}</td>
                <td><button className="danger-btn" onClick={() => onDelete(circle.id)}>Eliminar</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
