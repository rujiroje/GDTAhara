// =================================================================
// File: src/pages/Dashboard.jsx (ฉบับแก้ไข เพิ่ม Role Document/Management)
// =================================================================
import React from "react";
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
            <div className="user-greeting"> สวัสดี, <span className="user-name">{user.username}</span> <span className="user-role">({user.role})</span> </div>
            <button onClick={logout} className="logout-button"> ออกจากระบบ </button>
          </div>
        </div>
      </nav>
      <main className="dashboard-main">{renderDashboardByRole()}</main>
    </div>
  );
};

export default Dashboard;
