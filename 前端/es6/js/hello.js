// export const util = {
//     sum(a, b) {
//         return a + b;
//     }
// }
//这样的写法，导入的时候可以随意起名字了
export default {
    sum(a, b) {
        return a + b;
    }
}
//下面这种写法是导出了util，那边导入的时候也是导入util
// export {util}

//`export`不仅可以导出对象，一切JS变量都可以导出。比如：基本类型变量、函数、数组、对象。