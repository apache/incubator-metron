import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { PcapFiltersComponent } from './pcap-filters.component';
import { FormsModule } from '../../../../node_modules/@angular/forms';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { PcapRequest } from '../model/pcap.request';
import { emit } from 'cluster';

@Component({
  selector: 'app-date-picker',
  template: '<input type="text" [(value)]="date">',
})
class FakeDatePicker {
  @Input() date: string;
  @Output() dateChange = new EventEmitter<string>();
}

describe('PcapFiltersComponent', () => {
  let component: PcapFiltersComponent;
  let fixture: ComponentFixture<PcapFiltersComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule
      ],
      declarations: [ 
        FakeDatePicker,
        PcapFiltersComponent,
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PcapFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('From date should be bound to the component', () => {
    let input = fixture.debugElement.query(By.css('#startTime'));
    const dateString = '2020-11-11 11:11:11';
    input.componentInstance.dateChange.emit(dateString);
    fixture.detectChanges();
    
    expect(component.startTimeStr).toBe(dateString);
  });

  it('To date should be bound to the component', () => {
    let input = fixture.debugElement.query(By.css('#endTime'));
    const dateString = '2030-11-11 11:11:11';
    input.componentInstance.dateChange.emit(dateString);
    fixture.detectChanges();
    
    expect(component.endTimeStr).toBe(dateString);
  });

  it('IP Source Address should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[name="ipSrcAddr"]');
    input.value = '192.168.0.1';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.model.ipSrcAddr).toBe('192.168.0.1');
  });

  it('IP Source Port should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[name="ipSrcPort"]');
    input.value = '9345';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.model.ipSrcPort).toBe(9345);
  });

  it('IP Dest Address should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[name="ipDstAddr"]');
    input.value = '256.0.0.7';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.model.ipDstAddr).toBe('256.0.0.7');
  });
  
  it('IP Dest Port should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[name="ipDstPort"]');
    input.value = '8989';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    
    expect(component.model.ipDstPort).toBe(8989);
  });

  it('Protocol should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[name="protocol"]');
    input.value = 'TCP';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.model.protocol).toBe('TCP');
  });

  it('Include Reverse Traffic should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[name="includeReverse"]');
    input.click();
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.model.includeReverse).toBe(true);
  });

  it('Text filter should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[name="protocol"]');
    input.value = 'TestStringFilter';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.model.protocol).toBe('TestStringFilter');
  });

  it('From date should be converted to timestamp on submit', () => {
    component.startTimeStr = '2220-12-12 12:12:12';
    component.search.emit = (model: PcapRequest) => {
      expect(model.startTimeMs).toBe(7919118732000);
    }
    component.onSubmit();
  });

  it('To date should be converted to timestamp on submit', () => {
    component.endTimeStr = '2320-03-13 13:13:13';
    component.search.emit = (model: PcapRequest) => {
      expect(model.endTimeMs).toBe(11051122393000);
    }
    component.onSubmit();
  });

  it('Filter should have an output called search', () => {
    component.search.subscribe((filterModel) => {
      expect(filterModel).toBeDefined();
    });
    component.onSubmit();
  });

  it('Filter should emit search event on submit', () => {
    spyOn(component.search, 'emit');
    component.onSubmit();
    expect(component.search.emit).toHaveBeenCalled();
  });

  it('Search event should contains the filter model', () => {
    spyOn(component.search, 'emit');
    component.onSubmit();
    expect(component.search.emit).toHaveBeenCalledWith(component.model);
  });

  it('Filter model structure aka PcapRequest', () => {
    expect(fixture.componentInstance.model.hasOwnProperty('startTimeMs')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('endTimeMs')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('ipSrcAddr')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('ipSrcPort')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('ipDstAddr')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('ipDstPort')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('protocol')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('packetFilter')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('includeReverse')).toBeTruthy();
  });

});