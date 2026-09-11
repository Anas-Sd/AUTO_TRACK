const fs = require('fs');  
const path = require('path');  
function save(file, text) { const p = path.join(__dirname, file); fs.mkdirSync(path.dirname(p), {recursive:true}); fs.writeFileSync(p, text.trim(), 'utf8'); console.log('Saved:', file); }  
module.exports = save; 
