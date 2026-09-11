const fs = require('fs');  
const path = require('path');  
module.exports = function(file, code) {  
  const full = path.join(__dirname, file);  
  fs.mkdirSync(path.dirname(full), { recursive: true });  
  fs.writeFileSync(full, code.trim(), 'utf8');  
  console.log('Saved:', file);  
}; 
