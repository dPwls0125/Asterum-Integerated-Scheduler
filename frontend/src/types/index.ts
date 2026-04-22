export interface Team {
  id: number;
  name: string;
  description: string | null;
  members: Member[] | null;
}

export interface Member {
  id: number;
  name: string;
  role: 'ARTIST' | 'STAFF';
  position: string | null;
  team: Team | null;
}

export interface Resource {
  id: number;
  name: string;
  type: 'STUDIO' | 'REHEARSAL_ROOM' | 'RECORDING_STUDIO' | 'OUTDOOR';
}

export interface RecurrenceInfo {
  type: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  endType: 'UNTIL_DATE' | 'COUNT' | 'NEVER';
  endDate: string | null;
  endCount: number | null;
  startDate: string;
}

export interface Schedule {
  id: number | null;
  title: string;
  description: string | null;
  date: string;
  startTime: string;
  endTime: string;
  recurring: boolean;
  recurrenceGroupId: number | null;
  recurrence: RecurrenceInfo | null;
  participants: Member[];
  resource: Resource | null;
}

export interface ScheduleCreateRequest {
  title: string;
  description?: string;
  startDate: string;
  startTime: string;
  endTime: string;
  participantIds?: number[];
  teamIds?: number[];
  resourceId?: number | null;
  recurrence?: {
    type: 'DAILY' | 'WEEKLY' | 'MONTHLY';
    endType: 'UNTIL_DATE' | 'COUNT' | 'NEVER';
    endDate?: string;
    endCount?: number;
  } | null;
}

export interface ScheduleUpdateRequest {
  title?: string;
  description?: string;
  startTime?: string;
  endTime?: string;
  participantIds?: number[];
  teamIds?: number[];
  resourceId?: number | null;
}

export interface ConvertToRecurringRequest {
  recurrence: {
    type: 'DAILY' | 'WEEKLY' | 'MONTHLY';
    endType: 'UNTIL_DATE' | 'COUNT' | 'NEVER';
    endDate?: string;
    endCount?: number;
  };
}

export type EditScope = 'THIS' | 'THIS_AND_FOLLOWING' | 'ALL';
