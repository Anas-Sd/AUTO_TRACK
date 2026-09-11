const fs = require('fs');  
const path = require('path');  
const file = process.argv[2];  
const b64 = process.argv[3];  
const full = path.join(__dirname, file);  
fs.mkdirSync(path.dirname(full), { recursive: true });  
fs.writeFileSync(full, Buffer.from(b64, 'base64').toString('utf8'), 'utf8');  
console.log('Saved:', file); 
