import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { DashboardPage } from './pages/DashboardPage';
import { GroupChatsPage } from './pages/GroupChatsPage';
import { LoginPage } from './pages/LoginPage';
import { MessagesPage } from './pages/MessagesPage';
import { PrivateChatsPage } from './pages/PrivateChatsPage';
import { TrustCirclesPage } from './pages/TrustCirclesPage';
import { UsersPage } from './pages/UsersPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/usuarios" element={<UsersPage />} />
          <Route path="/grupos" element={<GroupChatsPage />} />
          <Route path="/privados" element={<PrivateChatsPage />} />
          <Route path="/mensajes" element={<MessagesPage />} />
          <Route path="/circulos" element={<TrustCirclesPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
