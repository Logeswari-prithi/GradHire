import "./App.css";
import { motion } from "framer-motion";

import {
  FaGithub,
  FaLinkedin,
  FaPython,
  FaJava,
  FaDatabase,
  FaPhone,
  FaEnvelope,
  FaReact,
  FaNodeJs,
  FaGitAlt,
} from "react-icons/fa";

function App() {
  return (
    <div>
      {/* NAVBAR */}

      <nav className="navbar">
        <div className="logo">Logeswari</div>

        <ul>
          <li>
            <a href="#about">About</a>
          </li>

          <li>
            <a href="#projects">Projects</a>
          </li>

          <li>
            <a href="#skills">Skills</a>
          </li>

          <li>
            <a href="#contact">Contact</a>
          </li>
        </ul>
      </nav>

      {/* HERO */}

      <section className="hero">
        <motion.div
          className="hero-content"
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1 }}
        >
          <h1>
            Hi, I'm <span>Logeswari</span>
          </h1>

          <h2>Software Developer & MCA Student</h2>

          <p>
            Passionate about Java development, AI-powered systems,
            backend technologies, and modern web applications.
          </p>

          <div className="hero-buttons">
            <a
              href="https://github.com/logeswari-prithi"
              target="_blank"
              rel="noreferrer"
            >
              GitHub
            </a>

            <a
              href="https://www.linkedin.com/in/logeswari-s-prithi"
              target="_blank"
              rel="noreferrer"
            >
              LinkedIn
            </a>
          </div>
        </motion.div>
      </section>

      {/* ABOUT */}

      <section id="about" className="section">
        <h2 className="title">About Me</h2>

        <div className="card">
          <p>
            I am an MCA student with strong interest in software development,
            Java application development, AI, machine learning, and database
            management. I enjoy building scalable and practical applications
            using Java, MySQL, MongoDB, and Spring Boot.
          </p>
        </div>
      </section>

      {/* PROJECTS */}

      <section id="projects" className="section">
        <h2 className="title">My Projects</h2>

        <div className="project-container">
          <motion.div
            className="project-card"
            whileHover={{ y: -8 }}
          >
            <h3>Lease Track - Rent Management System</h3>

            <p>
              Rent management application developed using .NET and SQL Server.
            </p>

            <ul>
              <li>Tenant Management</li>
              <li>Lease Tracking</li>
              <li>Rent Payment Records</li>
              <li>CRUD Operations</li>
            </ul>
          </motion.div>

          <motion.div
            className="project-card"
            whileHover={{ y: -8 }}
          >
            <h3>AI & Data Analytics Projects</h3>

            <p>
              Worked on AI, ML, and Data Analytics concepts during internship.
            </p>

            <ul>
              <li>Python Programming</li>
              <li>Machine Learning</li>
              <li>Data Analytics</li>
              <li>Problem Solving</li>
            </ul>
          </motion.div>
        </div>
      </section>

      {/* SKILLS */}

     <section id="skills" className="section">
        <h2 className="title">My Skills</h2>

        <div className="skills-container">
          <div className="skill-card">
            <FaJava />
            <span>Java</span>
          </div>

          <div className="skill-card">
            <FaPython />
            <span>Python</span>
          </div>

         {/* <div className="skill-card">
            <FaReact />
            <span>React</span>
          </div>

          <div className="skill-card">
            <FaNodeJs />
            <span>Node.js</span>
          </div> 

          <div className="skill-card">
            <FaGitAlt />
            <span>Git</span>
          </div> */}

          <div className="skill-card">
            <FaDatabase />
            <span>MySQL</span>
          </div>

          <div className="skill-card">
          <FaDatabase />
            <span>MongoDB</span>
          </div>

          <div className="skill-card">
            <FaJava />
            <span>Spring Boot</span>
          </div>
        </div>
      </section>

      {/* CONTACT */}

      <section id="contact" className="section">
        <h2 className="title">Contact Me</h2>

        <div className="contact-card">
          <p>
            <FaEnvelope /> logeswaris.revathi@gmail.com
          </p>

          <p>
            <FaPhone /> +91 9363119457
          </p>

          <div className="social-icons">
            <a
              href="https://github.com/logeswari-prithi"
              target="_blank"
              rel="noreferrer"
            >
              <FaGithub />
            </a>

            <a
              href="https://www.linkedin.com/in/logeswari-s-prithi"
              target="_blank"
              rel="noreferrer"
            >
              <FaLinkedin />
            </a>
          </div>
        </div>
      </section>
    </div>
  );
}

export default App;