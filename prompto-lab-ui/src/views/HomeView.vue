<template>
  <div class="home">
    <!-- 动态背景 -->
    <div class="dynamic-background">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
      <div class="floating-particles">
        <div class="particle" v-for="i in 20" :key="i"></div>
      </div>
    </div>

    <!-- 英雄区域 -->
    <section class="hero-section">
      <div class="container">
        <div class="hero-content">
          <div class="hero-left">
            <div class="hero-badge">
              <div class="badge-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <span class="badge-text">AI PROMPT ENGINEERING</span>
            </div>
            
            <h1 class="hero-title">
              <span class="title-main">PromptoLab</span>
              <span class="title-sub">智能提示词构建平台</span>
            </h1>
            
            <div class="hero-description">
              <p class="desc-main">
                通过智能对话引导，帮助用户构建专业级提示词，
                让AI真正理解您的意图，实现精准的智能交互。
              </p>
              <div class="feature-highlights">
                <div class="highlight-item">
                  <div class="highlight-icon">🎯</div>
                  <span>精准意图识别</span>
                </div>
                <div class="highlight-item">
                  <div class="highlight-icon">⚡</div>
                  <span>智能优化建议</span>
                </div>
                <div class="highlight-item">
                  <div class="highlight-icon">🚀</div>
                  <span>一键生成部署</span>
                </div>
              </div>
            </div>
            
            <div class="hero-actions">
              <router-link to="/prompt-builder" class="btn btn-primary">
                <span class="btn-text">开始构建</span>
                <div class="btn-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                    <path d="M5 12h14m-7-7l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </router-link>
              <button class="btn btn-secondary">
                <span class="btn-text">了解更多</span>
              </button>
            </div>
          </div>
          
          <div class="hero-right">
            <div class="hero-visual">
              <div class="visual-container">
                <div class="code-preview">
                  <div class="code-header">
                    <div class="code-controls">
                      <span class="control-dot red"></span>
                      <span class="control-dot yellow"></span>
                      <span class="control-dot green"></span>
                    </div>
                    <div class="code-title">prompt_builder.ai</div>
                  </div>
                  <div class="code-content">
                    <div class="code-line">
                      <span class="line-number">1</span>
                      <span class="code-text">
                        <span class="keyword">system</span>: 
                        <span class="string">"你是一个专业的AI助手..."</span>
                      </span>
                    </div>
                    <div class="code-line">
                      <span class="line-number">2</span>
                      <span class="code-text">
                        <span class="keyword">user</span>: 
                        <span class="string">"请帮我分析这个问题"</span>
                      </span>
                    </div>
                    <div class="code-line">
                      <span class="line-number">3</span>
                      <span class="code-text">
                        <span class="keyword">context</span>: 
                        <span class="string">"专业领域知识库"</span>
                      </span>
                    </div>
                    <div class="code-line typing">
                      <span class="line-number">4</span>
                      <span class="code-text">
                        <span class="keyword">output</span>: 
                        <span class="cursor">|</span>
                      </span>
                    </div>
                  </div>
                </div>
                <div class="floating-elements">
                  <div class="floating-card card-1">
                    <div class="card-icon">🤖</div>
                    <div class="card-text">AI分析</div>
                  </div>
                  <div class="floating-card card-2">
                    <div class="card-icon">📊</div>
                    <div class="card-text">数据洞察</div>
                  </div>
                  <div class="floating-card card-3">
                    <div class="card-icon">⚡</div>
                    <div class="card-text">实时优化</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 功能特性区域 -->
    <section class="features-section">
      <div class="container">
        <div class="section-header">
          <div class="section-badge">
            <span class="badge-dot"></span>
            <span class="badge-text">核心功能</span>
          </div>
          <h2 class="section-title">
            <span class="title-main">智能化提示词工程</span>
            <span class="title-sub">专为现代AI交互设计的完整解决方案</span>
          </h2>
        </div>
        
        <div class="features-grid">
          <div class="feature-card featured">
            <div class="card-header">
              <div class="card-icon-wrapper">
                <div class="card-icon">🧠</div>
              </div>
              <div class="card-status">
                <span class="status-indicator active"></span>
                <span class="status-text">运行中</span>
              </div>
            </div>
            <div class="card-content">
              <h3 class="card-title">智能对话引导</h3>
              <p class="card-description">
                基于先进的自然语言处理技术，通过智能对话逐步引导用户明确需求，
                自动分析意图并生成最优的提示词结构。
              </p>
              <div class="card-metrics">
                <div class="metric">
                  <div class="metric-value">98.5%</div>
                  <div class="metric-label">准确率</div>
                </div>
                <div class="metric">
                  <div class="metric-value">2.3min</div>
                  <div class="metric-label">平均时长</div>
                </div>
                <div class="metric">
                  <div class="metric-value">50K+</div>
                  <div class="metric-label">成功案例</div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="feature-card">
            <div class="card-header">
              <div class="card-icon-wrapper">
                <div class="card-icon">📚</div>
              </div>
              <div class="card-status">
                <span class="status-indicator"></span>
                <span class="status-text">就绪</span>
              </div>
            </div>
            <div class="card-content">
              <h3 class="card-title">专业模板库</h3>
              <p class="card-description">
                内置丰富的行业级提示词模板，覆盖商务、创意、技术等多个领域，
                支持自定义模板和团队协作。
              </p>
            </div>
          </div>
          
          <div class="feature-card">
            <div class="card-header">
              <div class="card-icon-wrapper">
                <div class="card-icon">⚡</div>
              </div>
              <div class="card-status">
                <span class="status-indicator"></span>
                <span class="status-text">就绪</span>
              </div>
            </div>
            <div class="card-content">
              <h3 class="card-title">实时性能优化</h3>
              <p class="card-description">
                基于AI反馈和用户行为数据，实时优化提示词效果，
                提供详细的性能分析和改进建议。
              </p>
            </div>
          </div>
          
          <div class="feature-card">
            <div class="card-header">
              <div class="card-icon-wrapper">
                <div class="card-icon">📈</div>
              </div>
              <div class="card-status">
                <span class="status-indicator"></span>
                <span class="status-text">就绪</span>
              </div>
            </div>
            <div class="card-content">
              <h3 class="card-title">深度效果分析</h3>
              <p class="card-description">
                提供全面的提示词效果分析报告，包括响应质量、
                用户满意度和优化建议等关键指标。
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 统计数据区域 -->
    <section class="stats-section">
      <div class="container">
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" fill="currentColor"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">99.9%</div>
              <div class="stat-label">系统可用性</div>
            </div>
          </div>
          
          <div class="stat-item">
            <div class="stat-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="9" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">50K+</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
          
          <div class="stat-item">
            <div class="stat-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                <path d="M9 11H5a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M21 11h-4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M7 21V8a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">1M+</div>
              <div class="stat-label">生成提示词</div>
            </div>
          </div>
          
          <div class="stat-item">
            <div class="stat-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" fill="currentColor"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">24/7</div>
              <div class="stat-label">智能服务</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA区域 -->
    <section class="cta-section">
      <div class="container">
        <div class="cta-content">
          <div class="cta-text">
            <h2 class="cta-title">
              <span class="title-main">准备开始您的AI之旅？</span>
              <span class="title-sub">立即体验智能提示词构建的强大功能</span>
            </h2>
          </div>
          <div class="cta-actions">
            <router-link to="/prompt-builder" class="btn btn-primary btn-large">
              <span class="btn-text">免费开始使用</span>
              <div class="btn-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M5 12h14m-7-7l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </router-link>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

// 动态背景效果
let animationId: number = 0

const initBackgroundAnimation = () => {
  const particles = document.querySelectorAll('.particle')
  
  particles.forEach((particle, index) => {
    const element = particle as HTMLElement
    const delay = Math.random() * 2
    const duration = 3 + Math.random() * 4
    const size = 2 + Math.random() * 4
    
    element.style.animationDelay = `${delay}s`
    element.style.animationDuration = `${duration}s`
    element.style.width = `${size}px`
    element.style.height = `${size}px`
    element.style.left = `${Math.random() * 100}%`
    element.style.top = `${Math.random() * 100}%`
  })
}

// 代码打字效果
const initTypingEffect = () => {
  const cursor = document.querySelector('.cursor')
  if (cursor) {
    setInterval(() => {
      cursor.classList.toggle('visible')
    }, 500)
  }
}

onMounted(() => {
  initBackgroundAnimation()
  initTypingEffect()
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<style scoped>
/* 基础样式 */
.home {
  min-height: 100vh;
  /* background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%); */
  color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  position: relative;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 32px;
}

/* 动态背景 */
.dynamic-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.3;
  animation: float 20s ease-in-out infinite;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(45deg, #d4af37, #ffd700);
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(45deg, #c0c0c0, #ffffff);
  top: 60%;
  right: 10%;
  animation-delay: -10s;
}

.orb-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(45deg, #d4af37, #b8860b);
  bottom: 20%;
  left: 50%;
  animation-delay: -5s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(30px, -30px) rotate(120deg);
  }
  66% {
    transform: translate(-20px, 20px) rotate(240deg);
  }
}

.floating-particles {
  position: absolute;
  width: 100%;
  height: 100%;
}

.particle {
  position: absolute;
  background: linear-gradient(45deg, #d4af37, #ffd700);
  border-radius: 50%;
  opacity: 0.6;
  animation: particle-float 6s ease-in-out infinite;
}

@keyframes particle-float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
    opacity: 0;
  }
  10%, 90% {
    opacity: 0.6;
  }
  50% {
    transform: translateY(-100px) rotate(180deg);
    opacity: 1;
  }
}

/* 英雄区域 */
.hero-section {
  padding: 120px 0 100px;
  position: relative;
  z-index: 2;
}

.hero-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: center;
}

.hero-left {
  max-width: 600px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(255, 215, 0, 0.1));
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 50px;
  padding: 12px 24px;
  margin-bottom: 32px;
  /* backdrop-filter: blur(10px); */
  transition: all 0.3s ease;
}

.hero-badge:hover {
  border-color: rgba(212, 175, 55, 0.6);
  box-shadow: 0 8px 32px rgba(212, 175, 55, 0.2);
}

.badge-icon {
  color: #d4af37;
  display: flex;
  align-items: center;
}

.badge-text {
  font-size: 14px;
  font-weight: 600;
  color: #d4af37;
  letter-spacing: 1px;
}

.hero-title {
  margin: 0 0 32px 0;
  line-height: 1.1;
}

.title-main {
  display: block;
  font-size: 4.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, #ffffff, #d4af37);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
  letter-spacing: -2px;
}

.title-sub {
  display: block;
  font-size: 1.5rem;
  color: #a0a0a0;
  font-weight: 400;
  letter-spacing: 0.5px;
}

.hero-description {
  margin-bottom: 48px;
}

.desc-main {
  font-size: 1.25rem;
  color: #e0e0e0;
  line-height: 1.7;
  margin-bottom: 32px;
  font-weight: 400;
}

.feature-highlights {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.highlight-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.highlight-item:hover {
  background: rgba(212, 175, 55, 0.1);
  border-color: rgba(212, 175, 55, 0.3);
  transform: translateY(-2px);
}

.highlight-icon {
  font-size: 1.2rem;
}

.highlight-item span {
  font-size: 14px;
  font-weight: 500;
  color: #e0e0e0;
}

.hero-actions {
  display: flex;
  gap: 20px;
  align-items: center;
}

/* 按钮样式 */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 16px 32px;
  border-radius: 12px;
  text-decoration: none;
  font-weight: 600;
  font-size: 16px;
  transition: all 0.3s ease;
  cursor: pointer;
  border: none;
  position: relative;
  overflow: hidden;
}

.btn-primary {
  background: linear-gradient(135deg, #d4af37, #ffd700);
  color: #000000;
  box-shadow: 0 8px 32px rgba(212, 175, 55, 0.3);
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 48px rgba(212, 175, 55, 0.4);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.2);
  /* backdrop-filter: blur(10px); */
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.btn-large {
  padding: 20px 40px;
  font-size: 18px;
}

.btn-text {
  font-weight: 600;
}

.btn-icon {
  display: flex;
  align-items: center;
  transition: transform 0.3s ease;
}

.btn:hover .btn-icon {
  transform: translateX(4px);
}

/* 英雄区域右侧视觉 */
.hero-right {
  position: relative;
}

.hero-visual {
  position: relative;
  height: 500px;
}

.visual-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.code-preview {
  background: linear-gradient(135deg, #1a1a1a, #2a2a2a);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  overflow: hidden;
  /* box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3); */
  /* backdrop-filter: blur(20px); */
}

.code-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.code-controls {
  display: flex;
  gap: 8px;
}

.control-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.control-dot.red {
  background: #ff5f57;
}

.control-dot.yellow {
  background: #ffbd2e;
}

.control-dot.green {
  background: #28ca42;
}

.code-title {
  font-size: 14px;
  color: #a0a0a0;
  font-weight: 500;
}

.code-content {
  padding: 24px;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
}

.code-line {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  min-height: 24px;
}

.line-number {
  width: 30px;
  color: #666;
  font-size: 14px;
  text-align: right;
  margin-right: 20px;
}

.code-text {
  font-size: 14px;
  line-height: 1.5;
}

.keyword {
  color: #d4af37;
  font-weight: 600;
}

.string {
  color: #a8e6cf;
}

.cursor {
  color: #d4af37;
  font-weight: bold;
  animation: blink 1s infinite;
}

.cursor.visible {
  opacity: 1;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.typing {
  position: relative;
}

.floating-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.floating-card {
  position: absolute;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(255, 215, 0, 0.1));
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 12px;
  padding: 16px 20px;
  /* backdrop-filter: blur(10px); */
  display: flex;
  align-items: center;
  gap: 12px;
  animation: float-card 6s ease-in-out infinite;
}

.card-1 {
  top: 20%;
  right: -20%;
  animation-delay: 0s;
}

.card-2 {
  bottom: 30%;
  left: -25%;
  animation-delay: -2s;
}

.card-3 {
  top: 60%;
  right: -15%;
  animation-delay: -4s;
}

@keyframes float-card {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
    opacity: 0.8;
  }
  50% {
    transform: translateY(-20px) rotate(2deg);
    opacity: 1;
  }
}

.floating-card .card-icon {
  font-size: 1.2rem;
}

.floating-card .card-text {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
}

/* 功能特性区域 */
.features-section {
  padding: 120px 0;
  position: relative;
  z-index: 2;
  background: rgba(0,0,0,0.4);
  /* backdrop-filter: blur(20px); */
}

.section-header {
  text-align: center;
  margin-bottom: 80px;
}

.section-badge {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: rgba(212, 175, 55, 0.1);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 50px;
  padding: 12px 24px;
  margin-bottom: 32px;
  /* backdrop-filter: blur(10px); */
}

.badge-dot {
  width: 8px;
  height: 8px;
  background: #d4af37;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
}

.section-badge .badge-text {
  font-size: 14px;
  font-weight: 600;
  color: #d4af37;
  letter-spacing: 1px;
}

.section-title {
  margin: 0;
}

.section-title .title-main {
  display: block;
  font-size: 3rem;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff, #d4af37);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
  letter-spacing: -1px;
}

.section-title .title-sub {
  display: block;
  font-size: 1.2rem;
  color: #a0a0a0;
  font-weight: 400;
}

.features-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 32px;
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
  /* backdrop-filter: blur(20px); */
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.05), transparent);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-card:hover {
  transform: translateY(-8px);
  border-color: rgba(212, 175, 55, 0.3);
  box-shadow: 0 20px 60px rgba(212, 175, 55, 0.1);
}

.feature-card.featured {
  grid-row: span 2;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(255, 215, 0, 0.05));
  border-color: rgba(212, 175, 55, 0.3);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.card-icon-wrapper {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.2), rgba(255, 215, 0, 0.1));
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.feature-card:hover .card-icon-wrapper {
  transform: scale(1.1);
  box-shadow: 0 8px 32px rgba(212, 175, 55, 0.2);
}

.card-icon {
  font-size: 1.8rem;
}

.card-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-indicator {
  width: 8px;
  height: 8px;
  background: #666;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.status-indicator.active {
  background: #28ca42;
  box-shadow: 0 0 10px rgba(40, 202, 66, 0.5);
}

.status-text {
  font-size: 12px;
  color: #a0a0a0;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.card-content {
  position: relative;
  z-index: 2;
}

.card-title {
  font-size: 1.5rem;
  color: #ffffff;
  margin: 0 0 16px 0;
  font-weight: 600;
}

.card-description {
  color: #c0c0c0;
  line-height: 1.6;
  margin-bottom: 24px;
  font-size: 15px;
}

.card-metrics {
  display: flex;
  gap: 32px;
}

.metric {
  text-align: center;
}

.metric-value {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  background: linear-gradient(135deg, #d4af37, #ffd700);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 12px;
  color: #a0a0a0;
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* 统计数据区域 */
.stats-section {
  padding: 80px 0;
  position: relative;
  z-index: 2;
  /* background: rgba(10, 10, 10, 0.5); */
  /* backdrop-filter: blur(20px); */
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 40px;
  max-width: 1000px;
  margin: 0 auto;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 32px 24px;
  /* background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02)); */
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  transition: all 0.3s ease;
  /* backdrop-filter: blur(20px); */
}

.stat-item:hover {
  transform: translateY(-8px);
  border-color: rgba(212, 175, 55, 0.3);
  box-shadow: 0 20px 60px rgba(212, 175, 55, 0.1);
}

.stat-icon {
  color: #d4af37;
  margin-bottom: 20px;
  transition: transform 0.3s ease;
}

.stat-item:hover .stat-icon {
  transform: scale(1.1);
}

.stat-content {
  width: 100%;
}

.stat-value {
  font-size: 2.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff, #d4af37);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  display: block;
}

.stat-label {
  font-size: 14px;
  color: #a0a0a0;
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* CTA区域 */
.cta-section {
  padding: 120px 0;
  position: relative;
  z-index: 2;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.05), rgba(10, 10, 10, 0.8));
  /* backdrop-filter: blur(20px); */
}

.cta-content {
  text-align: center;
  max-width: 800px;
  margin: 0 auto;
}

.cta-text {
  margin-bottom: 48px;
}

.cta-title .title-main {
  display: block;
  font-size: 3.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff, #d4af37);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
  letter-spacing: -1px;
}

.cta-title .title-sub {
  display: block;
  font-size: 1.3rem;
  color: #a0a0a0;
  font-weight: 400;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .container {
    padding: 0 24px;
  }
  
  .hero-content {
    gap: 60px;
  }
  
  .features-grid {
    grid-template-columns: 1fr 1fr;
    gap: 24px;
  }
  
  .feature-card.featured {
    grid-column: span 2;
    grid-row: span 1;
  }
}

@media (max-width: 768px) {
  .hero-content {
    grid-template-columns: 1fr;
    gap: 40px;
    text-align: center;
  }
  
  .title-main {
    font-size: 3.5rem;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .feature-card.featured {
    grid-column: span 1;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }
  
  .feature-highlights {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .container {
    padding: 0 20px;
  }
  
  .hero-section {
    padding: 80px 0 60px;
  }
  
  .title-main {
    font-size: 2.8rem;
  }
  
  .cta-title .title-main {
    font-size: 2.5rem;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .hero-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .btn {
    width: 100%;
    justify-content: center;
  }
  
  .feature-highlights {
    flex-direction: column;
    align-items: center;
  }
}
</style>