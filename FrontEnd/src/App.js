import React from 'react';
import NavigationBar from "./components/layouts/Navbar.js";
import Footer from './components/layouts/Footer.js';
import {Switch, Route, BrowserRouter} from 'react-router-dom';
import LoginPage from './components/pages/LoginPage';  
import HomePage from './components/pages/HomePage';

function App() {
  return (
    <div className="page-container">
      <div className="content-wrap"> 
        
        <BrowserRouter>
        <NavigationBar />
        <Switch> 
          <Route exact path="/" component={HomePage} />
          <Route path="/login" component={LoginPage} />
        </Switch>
        </BrowserRouter>
        
      </div>
      <Footer/>
    </div>
  );
}

export default App;
