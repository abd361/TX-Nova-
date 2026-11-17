
import './style.css'
import { registerPlugin } from '@capacitor/core'

const NovaBrowser = registerPlugin('NovaBrowser')

const app = document.querySelector('#app')

app.innerHTML = `
  <main class="shell">
    <header class="topbar">
      <div class="brand-mark">N</div>
      <div class="brand-copy">
        <div class="brand">TX <span>NOVA</span></div>
        <div class="tagline">A browser by Metrix Studio</div>
      </div>
      <button class="icon-btn" id="themeBtn">☼</button>
    </header>

    <section class="welcome">
      <div class="orb"><span>✦</span></div>
      <p class="eyebrow">YOUR WEB, YOUR WAY</p>
      <h1>Explore beyond.</h1>
      <p class="subtext">A fresh space for your everyday browsing.</p>

      <form class="search-box" id="searchForm">
        <span class="search-icon">⌕</span>
        <input id="addressInput" placeholder="Search or enter website">
        <button type="submit">➜</button>
      </form>

      <div class="shortcuts">
        <button class="shortcut" data-url="https://google.com">Google</button>
        <button class="shortcut" data-url="https://youtube.com">YouTube</button>
        <button class="shortcut" data-url="https://wikipedia.org">Wikipedia</button>
      </div>

      <button class="launch-btn" id="launchBtn">
        Open TX Nova Browser →
      </button>
    </section>

    <footer>TX NOVA • METRIX STUDIO</footer>
  </main>
`

function normalizeAddress(value) {
  const text = value.trim()
  if (!text) return ''

  if (text.startsWith('https://') || text.startsWith('http://')) {
    return text
  }

  const host = text.split('/')[0].split('?')[0].split('#')[0]

  if (/^[a-z0-9.-]+\.[a-z]{2,}$/i.test(host)) {
    return `https://${text}`
  }

  return `https://duckduckgo.com/?q=${encodeURIComponent(text)}`
}

async function openBrowser(value = '') {
  try {
    await NovaBrowser.open({
      url: normalizeAddress(value) || 'https://duckduckgo.com'
    })
  } catch (error) {
    console.error(error)
    alert('Native browser is not connected yet.')
  }
}

document.querySelector('#searchForm').addEventListener('submit', e => {
  e.preventDefault()
  openBrowser(document.querySelector('#addressInput').value)
})

document.querySelectorAll('.shortcut').forEach(button => {
  button.addEventListener('click', () => openBrowser(button.dataset.url))
})

document.querySelector('#launchBtn').addEventListener('click', () => {
  openBrowser()
})

document.querySelector('#themeBtn').addEventListener('click', () => {
  document.body.classList.toggle('light')
})
