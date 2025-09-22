import React from 'react';
import { formatDateTimeLocal, toServerIsoFromLocal, parseServerDateString } from '../utils/dateUtils';

type Suggestion = {
  nextSlots?: Array<{ start: string; end: string }>;
  alternateRooms?: Array<any>;
};

type Props = {
  show: boolean;
  onClose: () => void;
  suggestions: Suggestion | null;
  onQuickBook: (roomId: number, startIso: string, endIso: string) => void;
};

const SuggestionsModal: React.FC<Props> = ({ show, onClose, suggestions, onQuickBook }) => {
  if (!show || !suggestions) return null;

  return (
    <div className="modal d-block" tabIndex={-1} role="dialog" style={{ background: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-lg" role="document">
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">Suggested Alternatives</h5>
            <button type="button" className="btn-close" onClick={onClose}></button>
          </div>
          <div className="modal-body">
            {suggestions.nextSlots && suggestions.nextSlots.length > 0 && (
              <div className="mb-3">
                <h6>Next available time slots for requested room</h6>
                <ul>
                  {suggestions.nextSlots.map((s, idx) => (
                    <li key={idx}>{formatDateTimeLocal(s.start)} - {formatDateTimeLocal(s.end)}</li>
                  ))}
                </ul>
              </div>
            )}

            {suggestions.alternateRooms && suggestions.alternateRooms.length > 0 && (
              <div>
                <h6>Alternate rooms</h6>
                <div className="list-group">
                  {suggestions.alternateRooms
                    .slice()
                    .sort((a: any, b: any) => {
                      // primary: capacity closeness (abs diff)
                      const capDiffA = Math.abs((a.room.capacity || 0) - (a.requestedCapacity || 0));
                      const capDiffB = Math.abs((b.room.capacity || 0) - (b.requestedCapacity || 0));
                      if (capDiffA !== capDiffB) return capDiffA - capDiffB;
                      // secondary: location match (exact match preferred)
                      const locA = (a.room.location || '').toLowerCase().includes((a.requestedLocation || '').toLowerCase()) ? 0 : 1;
                      const locB = (b.room.location || '').toLowerCase().includes((b.requestedLocation || '').toLowerCase()) ? 0 : 1;
                      if (locA !== locB) return locA - locB;
                      // tertiary: amenities overlap (more overlap preferred)
                      const amenitiesA = new Set((a.room.amenities || []));
                      const amenitiesB = new Set((b.room.amenities || []));
                      const req = new Set((a.requestedAmenities || []));
                      let overlapA = 0;
                      let overlapB = 0;
                      req.forEach((r: any) => { if (amenitiesA.has(r)) overlapA++; if (amenitiesB.has(r)) overlapB++; });
                      return overlapB - overlapA; // more overlap first
                    })
                    .map((alt: any, idx: number) => {
                        const amenities: string[] = alt.room.amenities || [];
                        const requestedAmenities: string[] = alt.requestedAmenities || [];
                        const locationMatches = (String(alt.room.location) || '')
                        .toLowerCase()
                        .includes((String(alt.requestedLocation) || '').toLowerCase());

                        return(
                    <div key={idx} className="list-group-item d-flex justify-content-between align-items-center">
                      <div style={{minWidth: '0'}}>
                        <strong>{alt.room.name}</strong>
                        <div className="text-muted">Location: {alt.room.location} • Capacity: {alt.room.capacity}</div>
                        <div className="mt-1">
                            {Math.abs((alt.room.capacity || 0) - (alt.requestedCapacity || 0)) === 0 ? (
                                <span className="badge bg-success me-1">Capacity match</span>
                            ) : (
                                <span className="badge bg-secondary me-1">
                                Capacity ±{Math.abs((alt.room.capacity || 0) - (alt.requestedCapacity || 0))}
                                </span>
                            )}

                            {((String(alt.room.location) || '').toLowerCase().includes((String(alt.requestedLocation) || '').toLowerCase())) ? (
                                <span className="badge bg-success me-1">Location match</span>
                            ) : null}

                

                            {(amenities.filter(a => requestedAmenities.includes(a)).length > 0) ? (
                            <span className="badge bg-info me-1">Amenities match</span>
                            ) : null}
                             </div>
                        <div className="text-muted mt-2">Score: {(alt.score || 0).toFixed(2)} • Available from: {alt.availableFrom ? formatDateTimeLocal(alt.availableFrom) : 'N/A'}</div>
                      </div>
                      <div>
                          {alt.availableFrom ? (
                          <button className="btn btn-primary" onClick={() => {
                              const parsed = parseServerDateString(alt.availableFrom);
                              const end = parsed ? toServerIsoFromLocal(new Date(parsed.getTime() + 60*60*1000)) : null;
                              onQuickBook(alt.room.id, alt.availableFrom, end || alt.availableFrom);
                          }}>Quick book</button>
                        ) : (
                          <button className="btn btn-outline-secondary" disabled>Not Available</button>
                        )}
                      </div>
                    </div>
                  );}
                  )}
                </div>
              </div>
            )}
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Close</button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SuggestionsModal;
