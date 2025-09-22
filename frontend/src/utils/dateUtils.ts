// Utilities to normalize parsing and formatting of dates from server and local inputs
export function parseServerDateString(s?: string | null): Date | null {
  if (!s) return null;
  const str = String(s).trim();
  // If it contains timezone info (Z or + or - offset), use native parse
  if (str.endsWith('Z') || /[+-]\d{2}:?\d{2}$/.test(str)) {
    const d = new Date(str);
    return isNaN(d.getTime()) ? null : d;
  }
  // Otherwise assume server sent a local datetime like '2025-10-11T05:30:00'
  // Parse components and create a Date in the local timezone matching those fields
  const m = str.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/);
  if (m) {
    const year = parseInt(m[1], 10);
    const month = parseInt(m[2], 10) - 1;
    const day = parseInt(m[3], 10);
    const hour = parseInt(m[4], 10);
    const minute = parseInt(m[5], 10);
    const second = m[6] ? parseInt(m[6], 10) : 0;
    return new Date(year, month, day, hour, minute, second);
  }
  const d = new Date(str);
  return isNaN(d.getTime()) ? null : d;
}

export function toServerIsoFromLocal(d?: Date | null): string | null {
  if (!d) return null;
  // Return a server-local ISO-like string without timezone offset so backend interprets it as LocalDateTime
  const pad = (n: number) => n.toString().padStart(2, '0');
  const year = d.getFullYear();
  const month = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const hour = pad(d.getHours());
  const minute = pad(d.getMinutes());
  const second = pad(d.getSeconds());
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
}

export function formatDateLocal(dateStr?: string | null): string {
  const d = parseServerDateString(dateStr);
  return d ? d.toLocaleDateString() : '';
}

export function formatTimeLocal(dateStr?: string | null): string {
  const d = parseServerDateString(dateStr);
  return d ? d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
}

export function formatDateTimeLocal(dateStr?: string | null): string {
  const d = parseServerDateString(dateStr);
  return d ? d.toLocaleString() : '';
}
