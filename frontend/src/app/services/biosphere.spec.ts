import { TestBed } from '@angular/core/testing';

import { Biosphere } from './biosphere';

describe('Biosphere', () => {
  let service: Biosphere;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Biosphere);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
