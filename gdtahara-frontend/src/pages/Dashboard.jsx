// =================================================================
// File: src/pages/Dashboard.jsx (ฉบับแก้ไข เพิ่ม Role Document/Management)
// =================================================================
import React from "react";
import { t, getLang, setLang as setAppLang } from "../i18n/t";
import { useAuth } from "../App";

// --- Import Dashboard Components ---
import AdminDashboard from "../components/admin/AdminDashboard";
import ProductionControlDashboard from "../components/pc/ProductionControlDashboard";
import ShiftLeaderDashboard from "../components/shift-leader/ShiftLeaderDashboard";
import OperatorDashboard from "../components/operator/OperatorDashboard";
import TechnicianDashboard from "../components/technician/TechnicianDashboard";
import QaDashboard from "../components/qa/QaDashboard";
import CmOperatorDashboard from "../components/cm-operator/CmOperatorDashboard";
import DocMgmtDashboard from "../components/doc-mgmt/DocMgmtDashboard"; // Kept for backward-compat/import order, not used for now

// --- Dashboard Layout ---
const Dashboard = () => {
  const { user, logout } = useAuth();

  const [lang, setLangState] = React.useState(getLang());

  // Apply language: update state + persist + set document lang
  const applyLang = (newLang) => {
    setLangState(newLang);
    setAppLang(newLang);
    // No full reload; components using t() will re-render with new labels
  };

  const renderDashboardByRole = () => {
    switch (user.role) {
      case "DataAdmin":
      case "ADMIN":
      case "Admin": 
        return <AdminDashboard />;
      case "Production Control": 
        return <ProductionControlDashboard />;
      // Route Document/Management to the same PC dashboard to ensure identical behavior
      case "Management":
      case "Document":
        return <ProductionControlDashboard />;
      case "Shift Leader": 
        return <ShiftLeaderDashboard />;
      case "Operator": 
        return <OperatorDashboard />;
      case "Technician": 
        return <TechnicianDashboard />;
      case "QA": 
        return <QaDashboard />;
      case "CM Operator": 
        return <CmOperatorDashboard />;
      default:
        return ( <div className="dashboard-card"> <h2>ยินดีต้อนรับ, {user.username}</h2> <p>ยังไม่มีหน้าจอสำหรับบทบาท: {user.role}</p> </div> );
    }
  };

  return (
    <div>
      <nav className="dashboard-nav">
        <div className="nav-container">
          <h1 className="nav-brand">GDTahara</h1>
          <div className="nav-user-info">
            <div className="user-greeting"> {t('hello')} <span className="user-name">{user.username}</span> <span className="user-role">({user.role})</span> </div>
            {/* Language Switch: show both TH and EN, highlight active */}
            <div style={{ display: 'flex', gap: '0.25rem', marginRight: '0.5rem' }}>
              <button
                onClick={() => applyLang('th')}
                title="ภาษาไทย"
                style={{
                  padding: '0.35rem 0.6rem',
                  borderRadius: '6px',
                  border: '1px solid #ccc',
                  cursor: 'pointer',
                  fontWeight: 600,
                  backgroundColor: lang === 'th' ? '#005fb8' : '#fff',
                  color: lang === 'th' ? '#fff' : '#333'
                }}
              >
                TH
              </button>
              <button
                onClick={() => applyLang('en')}
                title="English"
                style={{
                  padding: '0.35rem 0.6rem',
                  borderRadius: '6px',
                  border: '1px solid #ccc',
                  cursor: 'pointer',
                  fontWeight: 600,
                  backgroundColor: lang === 'en' ? '#005fb8' : '#fff',
                  color: lang === 'en' ? '#fff' : '#333'
                }}
              >
                EN
              </button>
            </div>
            <button onClick={logout} className="logout-button"> {t('logout')} </button>
          </div>
        </div>
      </nav>
      <main className="dashboard-main">{renderDashboardByRole()}</main>
    </div>
  );
};

export default Dashboard;
