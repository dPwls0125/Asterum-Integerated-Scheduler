import { Schedule, ScheduleCreateRequest, ScheduleUpdateRequest, ConvertToRecurringRequest, EditScope, Member, Team, Resource } from '../types';

const BASE = '/api';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// ===== Members & Teams =====
export const fetchMembers = () => request<Member[]>('/members');
export const fetchTeams = () => request<Team[]>('/teams');

// ===== Resources =====
export const fetchResources = () => request<Resource[]>('/resources');

// ===== Schedules =====
export const fetchMonthlySchedules = (year: number, month: number) =>
  request<Schedule[]>(`/schedules?year=${year}&month=${month}`);

export const fetchScheduleDetail = (id: number) =>
  request<Schedule>(`/schedules/${id}`);

export const fetchRecurringInstanceDetail = (groupId: number, date: string) =>
  request<Schedule>(`/schedules/recurring/${groupId}/${date}`);

export const createSchedule = (data: ScheduleCreateRequest) =>
  request<Schedule>('/schedules', { method: 'POST', body: JSON.stringify(data) });

export const updateOneTimeSchedule = (id: number, data: ScheduleUpdateRequest) =>
  request<Schedule>(`/schedules/${id}`, { method: 'PUT', body: JSON.stringify(data) });

export const updateRecurringInstance = (groupId: number, date: string, scope: EditScope, data: ScheduleUpdateRequest) =>
  request<Schedule>(`/schedules/recurring/${groupId}/${date}?scope=${scope}`, { method: 'PUT', body: JSON.stringify(data) });

export const deleteOneTimeSchedule = (id: number) =>
  request<void>(`/schedules/${id}`, { method: 'DELETE' });

export const deleteRecurringInstance = (groupId: number, date: string, scope: EditScope) =>
  request<void>(`/schedules/recurring/${groupId}/${date}?scope=${scope}`, { method: 'DELETE' });

export const convertToRecurring = (id: number, data: ConvertToRecurringRequest) =>
  request<Schedule>(`/schedules/${id}/convert-to-recurring`, { method: 'POST', body: JSON.stringify(data) });
