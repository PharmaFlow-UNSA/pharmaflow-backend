<div align="center">
  <img src="https://cdn-icons-png.flaticon.com/512/883/883360.png" width="100" alt="PharmaFlow Logo">
  <h1>PharmaFlow: Advanced Microservices Ecosystem</h1>
  <p><i>An intelligent pharmaceutical management system built with Spring Boot Cloud architecture.</i></p>
</div>

<hr />

<h3>🌐 Overview</h3>
<p align="justify">
  <b>PharmaFlow</b> is a distributed backend system designed to modernize pharmaceutical interactions. By decoupling user health data from product management, the system ensures high availability, scalability, and real-time safety checks during the medication dispensing process.
</p>

<h3>🏗 Project Structure (Microservices)</h3>
<p>The ecosystem is divided into specialized services that communicate via REST/Feign:</p>

<table>
  <thead>
    <tr>
      <th>Service</th>
      <th>Responsibility</th>
      <th>Status</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><b>User & Health Service</b></td>
      <td>Identity management, Family hierarchies, and Clinical profiles (Allergies/Therapies).</td>
      <td>✅ Completed</td>
    </tr>
    <tr>
      <td><b>Product & Medical Service</b></td>
      <td>Drug catalogs, Active substances, and Interaction/Conflict logic.</td>
      <td>🚀 In Progress</td>
    </tr>
    <tr>
      <td><b>Notification Service</b></td>
      <td>Automated alerts for therapy schedules and interaction warnings.</td>
      <td>📅 Planned</td>
    </tr>
  </tbody>
</table>

<h3>🛠 Global Tech Stack</h3>
<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="Java">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.x-brightgreen.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-15-blue.svg" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Microservices-Architecture-red.svg" alt="Microservices">
  <img src="https://img.shields.io/badge/Security-BCrypt-lightgrey.svg" alt="Security">
</p>

---

<h3>📐 System Design</h3>
<p>Each service follows a strict domain-driven design with its own isolated database to prevent tight coupling.</p>

<div align="center">
  <img src="https://microservices.io/i/microservice-architecture.png" width="600" alt="Microservice Architecture Pattern">
  <p><i>Conceptual Microservice Workflow</i></p>
</div>

---

<h3>🚀 Quick Start (Local Development)</h3>
<ol>
  <li><b>Clone the ecosystem:</b>
    <pre><code>git clone https://github.com/tvoj-username/pharmaflow-backend.git</code></pre>
  </li>
  <li><b>Database Setup:</b> Ensure PostgreSQL is running. Each service will automatically seed its own test data (10 entities per service).</li>
  <li><b>Environment:</b> Set <code>DB_URL</code>, <code>DB_USERNAME</code> and <code>DB_PASSWORD</code> in your system environment.</li>
  <li><b>Run:</b> Navigate to the specific service folder and execute <code>mvn spring-boot:run</code>.</li>
</ol>

<hr />

<div align="center">
  <p><b>PharmaFlow Ecosystem</b> &copy; 2026</p>
  <sub>Designed and Developed for <b>Advanced Web Technologies</b></sub>
</div>
