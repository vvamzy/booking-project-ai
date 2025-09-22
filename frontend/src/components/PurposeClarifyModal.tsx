import React, { useState } from 'react';

type Props = {
    show: boolean;
    initialPurpose: string;
    rationale?: string[];
    onClose: () => void;
    onConfirm: (newPurpose: string) => void;
}

const PurposeClarifyModal: React.FC<Props> = ({ show, initialPurpose, rationale, onClose, onConfirm }) => {
    const [purpose, setPurpose] = useState(initialPurpose || '');

    // Keep local state in sync when modal reopens with different initialPurpose
    React.useEffect(() => { setPurpose(initialPurpose || ''); }, [initialPurpose, show]);

    if (!show) return null;

    return (
        <div className="modal-backdrop">
            <div className="modal-dialog modal-lg">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">Clarify Meeting Purpose</h5>
                        <button type="button" className="btn-close" aria-label="Close" onClick={onClose}></button>
                    </div>
                    <div className="modal-body">
                        <p>The booking purpose was flagged as unclear. Please clarify so the system can validate it.</p>
                        {rationale && rationale.length > 0 && (
                            <div className="alert alert-warning">
                                <strong>Reason(s):</strong>
                                <ul>
                                    {rationale.map((r, i) => <li key={i}>{r}</li>)}
                                </ul>
                            </div>
                        )}
                        <div className="mb-3">
                            <label className="form-label">Purpose</label>
                            <textarea className="form-control" rows={4} value={purpose} onChange={e => setPurpose(e.target.value)} />
                        </div>
                    </div>
                    <div className="modal-footer">
                        <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
                        <button className="btn btn-primary" onClick={() => onConfirm(purpose)}>Re-validate</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PurposeClarifyModal;
