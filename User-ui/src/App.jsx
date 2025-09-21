

import React from 'react';
import { BrowserRouter as Router, Routes, Route, Outlet } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { Toaster } from 'react-hot-toast';

// --- Context Providers ---
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';

// --- Layout & Helper Components ---
import Header from './component/Header-Footer/Header';
import Footer from './component/Header-Footer/Footer';
import ProtectedRoute from './component/Profilesidebar/ProtectedRoute';

// --- Page Components ---
import HomePage from './Pages/HomePage/HomePage';
import LoginPage from './Pages/Login-Register/LoginPage';
import RegisterPage from './Pages/Login-Register/RegisterPage';
import LoginSuccessPage from './Pages/Login-Register/LoginSuccessPage'; 
import ProductPage from './Pages/ProductPage/ProductPage';
import ProductDetailPage from './Pages/ProductPage/ProductDetailPage';
import CartPage from './Pages/CartPage/CartPage';
import PaymentSuccessPage from './Pages/Paymentpage/PaymentSuccessPage';
import PaymentCancelPage from './Pages/Paymentpage/PaymentCancelPage';
import PaymentFailedPage from './Pages/Paymentpage/PaymentFailedPage';
import SearchResultsPage from './Pages/ProductPage/SearchResultsPage';
import MyBuilds from './Pages/BuildPage/MyBuilds';
import PcBuilder from './Pages/BuildPage/PcBuilder';
import NotFoundPage from './Pages/HomePage/NotFoundPage';


// --- Profile Page & Nested Components ---
import ProfilePage from './Pages/ProfilePage/ProfilePage';
import ProfileInfo from './Pages/ProfilePage/ProfileInfo';
import ProfileEdit from './Pages/ProfilePage/ProfileEdit';
import AddressPage from './component/Address/AddressPage';
import UserOrders from './component/UserOrder/UserOrders';

// --- Styles ---
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

// Layout Component
const MainLayout = () => (
  <div className="app-container">
    <Header />
    <main className="main-content">
      <Outlet />
    </main>
    <Footer />
  </div>
);

function App() {
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  if (!googleClientId) {
    console.error("Fatal Error: VITE_GOOGLE_CLIENT_ID is not defined in the .env file.");
  }

  return (
    <GoogleOAuthProvider clientId={googleClientId || ""}>
      <AuthProvider>
        <CartProvider>
          <Toaster 
            position="bottom-right"
            toastOptions={{
              className: '',
              style: {
                border: '1px solid #713200',
                padding: '16px',
                color: '#713200',
              },
              success: { duration: 3000 },
            }}
          />
          <Router>
            <Routes>
              {/* === Routes ที่ใช้ MainLayout  */}
              <Route element={<MainLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/products/category/:categoryName" element={<ProductPage />} />
                <Route path="/products/:productId" element={<ProductDetailPage />} />
                <Route path="/search" element={<SearchResultsPage />} />
                <Route path="/cart" element={<CartPage />} />
                <Route path="/builds" element={<ProtectedRoute><MyBuilds /></ProtectedRoute>} />
                <Route path="/build/new" element={<ProtectedRoute><PcBuilder /></ProtectedRoute>} />
                <Route path="/build/:buildId" element={<ProtectedRoute><PcBuilder /></ProtectedRoute>} />
                <Route path="/payment-successful" element={<ProtectedRoute><PaymentSuccessPage /></ProtectedRoute>} />
                <Route path="/payment-cancelled" element={<ProtectedRoute><PaymentCancelPage /></ProtectedRoute>} />
                <Route path="/payment-failed" element={<PaymentFailedPage />} />
                <Route
                  path="/profile"
                  element={<ProtectedRoute><ProfilePage /></ProtectedRoute>}
                >
                  <Route index element={<ProfileInfo />} />
                  <Route path="edit" element={<ProfileEdit />} />
                  <Route path="orders" element={<UserOrders />} />
                  <Route path="address" element={<AddressPage />} />
                  <Route path="builds" element={<MyBuilds />} /> 
                </Route>
              </Route>

              {/* === Standalone Routes  */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              {/* Route สำหรับจัดการเมื่อ Google Login สำเร็จ  */}
              <Route path="/login-success" element={<LoginSuccessPage />} />
              
              {/* --- 404 Not Found Route --- */}
              <Route path="*" element={<NotFoundPage />} /> 
            </Routes>
          </Router>
        </CartProvider>
      </AuthProvider>
    </GoogleOAuthProvider>
  );
}

export default App;