import { useState } from 'react';
import { Schedule, Member, Team, Resource, EditScope, ConvertToRecurringRequest } from '../../types';
import * as api from '../../api/scheduleApi';
import EditScopeDialog from './EditScopeDialog';
import { X, Clock, MapPin, Users, Repeat, ArrowRightLeft, Calendar, Trash2, Pencil } from 'lucide-react';

interface Props {
  schedule: Schedule;
  members: Member[];
  teams: Team[];
  resources: Resource[];
  onClose: () => void;
  onEdit: (schedule: Schedule) => void;
  onDeleted: () => void;
  onConverted: () => void;
  onError: (msg: string) => void;
}

const REC_TYPE_LABELS: Record<string, string> = {
  DAILY: '매일', WEEKLY: '매주', MONTHLY: '매월',
};

const END_TYPE_LABELS: Record<string, string> = {
  NEVER: '무기한', UNTIL_DATE: '날짜까지', COUNT: '횟수 지정',
};

export default function ScheduleDetail({ schedule, members, teams, resources, onClose, onEdit, onDeleted, onConverted, onError }: Props) {
  const [showDeleteScope, setShowDeleteScope] = useState(false);
  const [showConvert, setShowConvert] = useState(false);
  const [convRecType, setConvRecType] = useState<'DAILY' | 'WEEKLY' | 'MONTHLY'>('WEEKLY');
  const [convEndType, setConvEndType] = useState<'NEVER' | 'UNTIL_DATE' | 'COUNT'>('NEVER');
  const [convEndDate, setConvEndDate] = useState('');
  const [convEndCount, setConvEndCount] = useState(10);

  const handleDelete = async (scope?: EditScope) => {
    try {
      if (schedule.recurring && schedule.recurrenceGroupId) {
        await api.deleteRecurringInstance(schedule.recurrenceGroupId, schedule.date, scope || 'THIS');
      } else if (schedule.id) {
        await api.deleteOneTimeSchedule(schedule.id);
      }
      onDeleted();
    } catch (e: any) {
      onError(e.message || '삭제에 실패했습니다.');
    }
  };

  const handleDeleteClick = () => {
    if (schedule.recurring) {
      setShowDeleteScope(true);
    } else {
      handleDelete();
    }
  };

  const handleConvert = async () => {
    if (!schedule.id) { onError('가상 인스턴스는 전환할 수 없습니다.'); return; }
    try {
      const req: ConvertToRecurringRequest = {
        recurrence: {
          type: convRecType,
          endType: convEndType,
          endDate: convEndType === 'UNTIL_DATE' ? convEndDate : undefined,
          endCount: convEndType === 'COUNT' ? convEndCount : undefined,
        }
      };
      await api.convertToRecurring(schedule.id, req);
      onConverted();
    } catch (e: any) {
      onError(e.message || '전환에 실패했습니다.');
    }
  };

  if (showDeleteScope) {
    return (
      <EditScopeDialog
        action="삭제"
        onSelect={(scope) => { handleDelete(scope); setShowDeleteScope(false); }}
        onClose={() => setShowDeleteScope(false)}
      />
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 480 }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <h3>{schedule.title}</h3>
            <span className={`detail-badge ${schedule.recurring ? 'recurring' : 'onetime'}`}>
              {schedule.recurring ? <><Repeat size={12} /> 반복</> : '일회성'}
            </span>
          </div>
          <button className="modal-close" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {/* Date & Time */}
          <div className="detail-section">
            <div className="detail-label"><Calendar size={12} style={{ verticalAlign: 'middle' }} /> 날짜</div>
            <div className="detail-value">{schedule.date}</div>
          </div>

          <div className="detail-section">
            <div className="detail-label"><Clock size={12} style={{ verticalAlign: 'middle' }} /> 시간</div>
            <div className="detail-time">
              {schedule.startTime?.slice(0, 5)} — {schedule.endTime?.slice(0, 5)}
            </div>
          </div>

          {/* Description */}
          {schedule.description && (
            <div className="detail-section">
              <div className="detail-label">설명</div>
              <div className="detail-value" style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                {schedule.description}
              </div>
            </div>
          )}

          {/* Resource */}
          {schedule.resource && (
            <div className="detail-section">
              <div className="detail-label"><MapPin size={12} style={{ verticalAlign: 'middle' }} /> 장소</div>
              <span className="detail-badge resource-badge">{schedule.resource.name}</span>
            </div>
          )}

          {/* Participants */}
          {schedule.participants && schedule.participants.length > 0 && (
            <div className="detail-section">
              <div className="detail-label"><Users size={12} style={{ verticalAlign: 'middle' }} /> 참여자</div>
              <div className="detail-participants">
                {schedule.participants.map(p => (
                  <span key={p.id}
                        className={`participant-chip selected ${p.role === 'ARTIST' ? 'artist' : ''}`}>
                    <span className="dot" />
                    {p.name}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Recurrence Info */}
          {schedule.recurrence && (
            <div className="detail-section">
              <div className="detail-label"><Repeat size={12} style={{ verticalAlign: 'middle' }} /> 반복 정보</div>
              <div className="detail-value" style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                {REC_TYPE_LABELS[schedule.recurrence.type]} · {END_TYPE_LABELS[schedule.recurrence.endType]}
                {schedule.recurrence.endDate && ` (${schedule.recurrence.endDate}까지)`}
                {schedule.recurrence.endCount && ` (${schedule.recurrence.endCount}회)`}
              </div>
            </div>
          )}

          {/* Convert to Recurring */}
          {!schedule.recurring && schedule.id && !showConvert && (
            <div style={{ marginTop: 16 }}>
              <button className="btn btn-secondary btn-sm" onClick={() => setShowConvert(true)}>
                <ArrowRightLeft size={14} /> 반복 일정으로 전환
              </button>
            </div>
          )}

          {showConvert && (
            <div className="recurrence-settings" style={{ marginTop: 12 }}>
              <div style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: 12, color: 'var(--accent-violet-light)' }}>
                반복 일정으로 전환
              </div>
              <div className="form-group">
                <label className="form-label">반복 주기</label>
                <select className="form-select" value={convRecType}
                        onChange={e => setConvRecType(e.target.value as any)}>
                  <option value="DAILY">매일</option>
                  <option value="WEEKLY">매주</option>
                  <option value="MONTHLY">매월</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">종료 조건</label>
                <select className="form-select" value={convEndType}
                        onChange={e => setConvEndType(e.target.value as any)}>
                  <option value="NEVER">무기한 반복</option>
                  <option value="UNTIL_DATE">특정 날짜까지</option>
                  <option value="COUNT">횟수 지정</option>
                </select>
              </div>
              {convEndType === 'UNTIL_DATE' && (
                <div className="form-group">
                  <label className="form-label">종료 날짜</label>
                  <input className="form-input" type="date" value={convEndDate}
                         onChange={e => setConvEndDate(e.target.value)} />
                </div>
              )}
              {convEndType === 'COUNT' && (
                <div className="form-group">
                  <label className="form-label">반복 횟수</label>
                  <input className="form-input" type="number" min={1} value={convEndCount}
                         onChange={e => setConvEndCount(Number(e.target.value))} />
                </div>
              )}
              <div className="btn-group">
                <button className="btn btn-secondary btn-sm" onClick={() => setShowConvert(false)}>취소</button>
                <button className="btn btn-primary btn-sm" onClick={handleConvert}>전환</button>
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="btn btn-danger btn-sm" onClick={handleDeleteClick}>
            <Trash2 size={14} /> 삭제
          </button>
          <button className="btn btn-primary btn-sm" onClick={() => onEdit(schedule)}>
            <Pencil size={14} /> 수정
          </button>
        </div>
      </div>
    </div>
  );
}
