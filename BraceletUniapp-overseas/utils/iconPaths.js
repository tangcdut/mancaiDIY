/**
 * Flat 2D line SVG icon paths
 * All icons are 24x24 viewBox, 2px stroke, round caps/joins
 * Each function returns a full data:image/svg+xml URI
 */

const svg = (body) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${body}</svg>`

const dataUri = (body, color = '#666') =>
  `data:image/svg+xml,${encodeURIComponent(svg(body).replace('<svg', `<svg stroke="${color}"`))}`

// ==================== Order Status Icons ====================
export const orderPending = (color = '#FF9800') => dataUri(
  '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>', color)

export const orderPaid = (color = '#4CAF50') => dataUri(
  '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>', color)

export const orderShipped = (color = '#2196F3') => dataUri(
  '<rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>', color)

export const orderCompleted = (color = '#FFC107') => dataUri(
  '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>', color)

// ==================== Navigation / General Icons ====================
export const location = (color = '#666') => dataUri(
  '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>', color)

export const service = (color = '#666') => dataUri(
  '<path d="M3 18v-6a9 9 0 0 1 18 0v6"/><path d="M21 19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1h18v1z"/><path d="M3 15h18"/>', color)

export const settings = (color = '#666') => dataUri(
  '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>', color)

export const phone = (color = '#666') => dataUri(
  '<rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>', color)

export const chat = (color = '#666') => dataUri(
  '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>', color)

export const lock = (color = '#999') => dataUri(
  '<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>', color)

export const ruler = (color = '#666') => dataUri(
  '<polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/><line x1="4" y1="12" x2="20" y2="12"/>', color)

export const trash = (color = '#e74c3c') => dataUri(
  '<polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>', color)

export const close = (color = '#999') => dataUri(
  '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>', color)

export const tap = (color = '#666') => dataUri(
  '<path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"/><line x1="7" y1="22" x2="7" y2="11"/>', color)

export const star = (color = '#FFC107') => dataUri(
  '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>', color)

export const smile = (color = '#666') => dataUri(
  '<circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/>', color)

// ==================== About Page Feature Icons ====================
export const diamond = (color = '#666') => dataUri(
  '<polygon points="12 2 22 8.5 12 15 2 8.5"/><polygon points="12 15 22 8.5 12 22 2 8.5"/>', color)

export const sparkle = (color = '#FFC107') => dataUri(
  '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>', color)

export const info = (color = '#666') => dataUri(
  '<circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>', color)

export const check = (color = '#4CAF50') => dataUri(
  '<polyline points="20 6 9 17 4 12"/>', color)

export const palette = (color = '#666') => dataUri(
  '<circle cx="13.5" cy="6.5" r="1.5"/><circle cx="17.5" cy="10.5" r="1.5"/><circle cx="8.5" cy="7.5" r="1.5"/><circle cx="6.5" cy="12.5" r="1.5"/><path d="M12 2C6.49 2 2 6.49 2 12s4.49 10 10 10a2 2 0 0 0 2-2c0-.52-.2-1-.53-1.37-.33-.36-.47-.82-.47-1.28 0-1.1.9-2 2-2h2.35c3.04 0 5.5-2.46 5.5-5.5C22.85 5.82 18.15 2 12 2z"/>', color)
