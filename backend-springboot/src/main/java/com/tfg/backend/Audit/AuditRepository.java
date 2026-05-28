package com.tfg.backend.Audit;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

// Append-only: solo inserción (save) y lectura (JpaSpecificationExecutor).
// No se expone delete ni update, de modo que las filas de auditoría no pueden
// modificarse ni borrarse a través de este contrato.
public interface AuditRepository extends Repository<Audit, Long>, JpaSpecificationExecutor<Audit> {
    Audit save(Audit audit);
}
