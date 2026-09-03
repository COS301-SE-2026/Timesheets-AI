import http from 'k6/http';
import {sleep, check} from 'k6';

export const options = {

    // different stages with different virtual users 
    
    // THIS IS TESTING ****LOAD TEST AND SPIKE TEST *****
    stages: [
        { duration: '30s', target: 10},
        { duration: '1m', target: 10},
        { duration: '30s', target: 50},
        { duration: '1m', target: 50},
        { duration: '30s', target: 100},
        { duration: '2ms', target: 100},
        { duration: '30s', target: 0},
    ], 

    thresholds: {
        // less than 1% requests should fail 
        http_req_failed: ['rate<0.01'],
        // 95% requests should be don in 250 milliseconds and 99 should be done in 1 seconds 
        http_req_duration: ['p(95)<250', 'p(99)<1000'],
    }, 
};

export default function (){
    const request = http.get('http://momentlyapp.org');
    check(res, {
        'status is 200': (response) => response.status === 200,
    });

    sleep(1);
}